package com.lin0721.linmusic.core.reccobeats

import android.util.LruCache
import com.lin0721.linmusic.core.log.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

data class ReccoBeatsAudioFeatures(
    val id: String,
    val mimikyuId: String? = null,
    val acousticness: Double = 0.0,
    val danceability: Double = 0.0,
    val energy: Double = 0.0,
    val instrumentalness: Double = 0.0,
    val key: Int = 0,
    val liveness: Double = 0.0,
    val loudness: Double = 0.0,
    val mode: Int = 0,
    val speechiness: Double = 0.0,
    val tempo: Double = 0.0,
    val valence: Double = 0.0,
)

data class ReccoBeatsCandidate(
    val id: String,
    val title: String,
    val artist: String,
    val mimikyuId: String? = null,
    val durationMs: Long = 0L,
    val href: String? = null,
)

/**
 * ReccoBeats recommendation repository (base URL: https://api.reccobeats.com/v1).
 * Requires no API key.
 * Guarantees:
 * - Silent null / empty returns on 404, 400, or failure (never throws).
 * - Fast in-memory caching for repeat lookups.
 * - Graceful degradation to existing similarity/automix pipelines.
 */
class ReccoBeatsRepository {

    companion object {
        private const val TAG = "ReccoBeats"
        private const val BASE_URL = "https://api.reccobeats.com/v1"
        private const val USER_AGENT = "MimikyuPlayer/1.0"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(7, TimeUnit.SECONDS)
        .build()

    // In-memory cache for audio features: mimikyuTrackId -> ReccoBeatsAudioFeatures
    private val audioFeaturesCache = LruCache<String, ReccoBeatsAudioFeatures>(200)

    // In-memory cache for recommendations: seedKey -> List<ReccoBeatsCandidate>
    private val recommendationsCache = LruCache<String, List<ReccoBeatsCandidate>>(100)

    // In-memory mapping from mimikyuId / query -> reccoBeatsTrackId
    private val mimikyuToReccoIdMap = LruCache<String, String>(500)

    /**
     * Resolves a Mimikyu track ID to its ReccoBeats UUID.
     */
    private suspend fun resolveMimikyuTrackId(mimikyuTrackId: String): String? = withContext(Dispatchers.IO) {
        val cleanId = mimikyuTrackId.trim().removePrefix("mimikyu:track:")
        if (cleanId.isBlank()) return@withContext null

        mimikyuToReccoIdMap.get(cleanId)?.let { return@withContext it }

        runCatching {
            val url = "$BASE_URL/track?ids=${URLEncoder.encode(cleanId, StandardCharsets.UTF_8.name())}"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val body = response.body?.string() ?: return@runCatching null
                val root = JSONObject(body)
                val content = root.optJSONArray("content") ?: return@runCatching null
                if (content.length() == 0) return@runCatching null

                val item = content.getJSONObject(0)
                val reccoId = item.optString("id").takeIf { it.isNotBlank() }
                if (reccoId != null) {
                    mimikyuToReccoIdMap.put(cleanId, reccoId)
                }
                reccoId
            }
        }.getOrNull()
    }

    /**
     * Resolves a track to its ReccoBeats UUID using the official /v1/track/search endpoint.
     */
    suspend fun resolveTrackIdBySearch(query: String): String? = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.length < 2) return@withContext null
        val cacheKey = "recco_search:${clean.lowercase()}"
        mimikyuToReccoIdMap.get(cacheKey)?.let { return@withContext it }

        // Try exact query first, then fallback to title part if multi-word
        val terms = listOf(clean) + if (clean.contains(" ")) listOf(clean.split(" ").take(2).joinToString(" ")) else emptyList()

        for (term in terms) {
            val resolvedId = runCatching {
                val url = "$BASE_URL/track/search?searchText=${URLEncoder.encode(term, StandardCharsets.UTF_8.name())}&size=2"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    val body = response.body?.string() ?: return@runCatching null
                    val root = JSONObject(body)
                    val content = root.optJSONArray("content") ?: return@runCatching null
                    if (content.length() == 0) return@runCatching null

                    val first = content.getJSONObject(0)
                    first.optString("id").takeIf { it.isNotBlank() }
                }
            }.getOrNull()

            if (resolvedId != null) {
                mimikyuToReccoIdMap.put(cacheKey, resolvedId)
                return@withContext resolvedId
            }
        }
        null
    }

    /**
     * Resolves multiple Mimikyu track IDs to their ReccoBeats UUIDs in a single batch request.
     */
    private suspend fun resolveBatchMimikyuIds(mimikyuTrackIds: List<String>): Map<String, String> = withContext(Dispatchers.IO) {
        val cleanIds = mimikyuTrackIds.map { it.trim().removePrefix("mimikyu:track:") }.filter { it.isNotBlank() }.distinct()
        if (cleanIds.isEmpty()) return@withContext emptyMap()

        val result = mutableMapOf<String, String>()
        val missing = mutableListOf<String>()

        for (id in cleanIds) {
            val cached = mimikyuToReccoIdMap.get(id)
            if (cached != null) {
                result[id] = cached
            } else {
                missing.add(id)
            }
        }

        if (missing.isEmpty()) return@withContext result

        // Batch up to 20 IDs per request
        missing.chunked(20).forEach { chunk ->
            runCatching {
                val queryIds = chunk.joinToString(",") { URLEncoder.encode(it, StandardCharsets.UTF_8.name()) }
                val url = "$BASE_URL/track?ids=$queryIds"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        val root = JSONObject(body)
                        val content = root.optJSONArray("content")
                        if (content != null) {
                            for (i in 0 until content.length()) {
                                val item = content.getJSONObject(i)
                                val reccoId = item.optString("id")
                                val href = item.optString("href")
                                val itemMimikyuId = href.substringAfterLast("/").takeIf { it.isNotBlank() }
                                if (!reccoId.isNullOrBlank() && itemMimikyuId != null) {
                                    mimikyuToReccoIdMap.put(itemMimikyuId, reccoId)
                                    result[itemMimikyuId] = reccoId
                                }
                            }
                        }
                    }
                }
            }
        }

        result
    }

    /**
     * Fetches audio features for a Mimikyu track ID (energy, valence, danceability, tempo, etc.).
     */
    suspend fun fetchAudioFeatures(mimikyuTrackId: String): ReccoBeatsAudioFeatures? = withContext(Dispatchers.IO) {
        val cleanId = mimikyuTrackId.trim().removePrefix("mimikyu:track:")
        if (cleanId.isBlank()) return@withContext null

        audioFeaturesCache.get(cleanId)?.let { return@withContext it }

        val reccoId = resolveMimikyuTrackId(cleanId) ?: return@withContext null

        runCatching {
            val url = "$BASE_URL/track/$reccoId/audio-features"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val body = response.body?.string() ?: return@runCatching null
                val root = JSONObject(body)

                val features = ReccoBeatsAudioFeatures(
                    id = root.optString("id", reccoId),
                    mimikyuId = cleanId,
                    acousticness = root.optDouble("acousticness", 0.0),
                    danceability = root.optDouble("danceability", 0.0),
                    energy = root.optDouble("energy", 0.0),
                    instrumentalness = root.optDouble("instrumentalness", 0.0),
                    key = root.optInt("key", 0),
                    liveness = root.optDouble("liveness", 0.0),
                    loudness = root.optDouble("loudness", 0.0),
                    mode = root.optInt("mode", 0),
                    speechiness = root.optDouble("speechiness", 0.0),
                    tempo = root.optDouble("tempo", 0.0),
                    valence = root.optDouble("valence", 0.0),
                )
                audioFeaturesCache.put(cleanId, features)
                features
            }
        }.onFailure {
            AppLogger.d(TAG, "Failed to fetch audio features for $cleanId: ${it.message}")
        }.getOrNull()
    }

    /**
     * Calls the ReccoBeats Recommendation endpoint with seed queries (titles and artists).
     * Resolves their Recco UUIDs and fetches AI/machine-learning recommendations.
     */
    suspend fun getRecommendationsBySeedQueries(
        seedQueries: List<String>,
        limit: Int = 15
    ): List<ReccoBeatsCandidate> = withContext(Dispatchers.IO) {
        if (seedQueries.isEmpty()) return@withContext emptyList()

        val cleanQueries = seedQueries.filter { it.isNotBlank() }.take(5)
        val cacheKey = "reco_seeds:" + cleanQueries.sorted().joinToString("|") + ":$limit"
        recommendationsCache.get(cacheKey)?.let { return@withContext it }

        val seedReccoIds = cleanQueries.mapNotNull { query ->
            resolveTrackIdBySearch(query)
        }.distinct()

        if (seedReccoIds.isEmpty()) {
            AppLogger.d(TAG, "No seed IDs could be resolved on ReccoBeats for: $cleanQueries")
            return@withContext emptyList()
        }

        runCatching {
            val seedsParam = seedReccoIds.joinToString(",")
            val url = "$BASE_URL/track/recommendation?seeds=$seedsParam&size=$limit"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    AppLogger.d(TAG, "Recommendation endpoint returned status ${response.code}")
                    return@runCatching emptyList()
                }
                val body = response.body?.string() ?: return@runCatching emptyList()
                val root = JSONObject(body)
                val content = root.optJSONArray("content") ?: return@runCatching emptyList()

                val candidates = mutableListOf<ReccoBeatsCandidate>()
                for (i in 0 until content.length()) {
                    val item = content.getJSONObject(i)
                    val id = item.optString("id")
                    val title = item.optString("trackTitle")
                    val artistsArray = item.optJSONArray("artists")
                    val artistName = artistsArray?.optJSONObject(0)?.optString("name").orEmpty()
                    val href = item.optString("href")
                    val candidateMimikyuId = href.substringAfterLast("/").takeIf { it.isNotBlank() }

                    if (title.isNotBlank() && artistName.isNotBlank()) {
                        candidates.add(
                            ReccoBeatsCandidate(
                                id = id,
                                title = title,
                                artist = artistName,
                                mimikyuId = candidateMimikyuId,
                                durationMs = item.optLong("durationMs", 0L),
                                href = href,
                            )
                        )
                    }
                }

                recommendationsCache.put(cacheKey, candidates)
                candidates
            }
        }.onFailure {
            AppLogger.d(TAG, "Recco recommendation query failed: ${it.message}")
        }.getOrDefault(emptyList())
    }

    /**
     * Calls the ReccoBeats Recommendation endpoint with seed Mimikyu track IDs or Recco UUIDs.
     * Returns candidate tracks (with their Mimikyu IDs + basic metadata).
     */
    suspend fun fetchRecommendations(
        seedMimikyuTrackIds: List<String>,
        limit: Int = 20
    ): List<ReccoBeatsCandidate> = withContext(Dispatchers.IO) {
        if (seedMimikyuTrackIds.isEmpty()) return@withContext emptyList()

        val cleanSeeds = seedMimikyuTrackIds
            .map { it.trim().removePrefix("mimikyu:track:") }
            .filter { it.isNotBlank() }
            .take(5)

        val cacheKey = cleanSeeds.sorted().joinToString(",") + ":$limit"
        recommendationsCache.get(cacheKey)?.let { return@withContext it }

        // If seeds are already UUIDs (e.g. 36 chars with dashes), use directly
        val uuidSeeds = cleanSeeds.filter { it.length == 36 && it.contains("-") }
        val mimikyuSeeds = cleanSeeds.filterNot { it.length == 36 && it.contains("-") }

        val resolvedMap = if (mimikyuSeeds.isNotEmpty()) resolveBatchMimikyuIds(mimikyuSeeds) else emptyMap()
        val seedReccoIds = (uuidSeeds + mimikyuSeeds.mapNotNull { resolvedMap[it] }).distinct()

        if (seedReccoIds.isEmpty()) {
            AppLogger.d(TAG, "Could not resolve any seed IDs in ReccoBeats database")
            return@withContext emptyList()
        }

        runCatching {
            val seedsParam = seedReccoIds.joinToString(",")
            val url = "$BASE_URL/track/recommendation?seeds=$seedsParam&size=$limit"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    AppLogger.d(TAG, "Recommendation endpoint returned status ${response.code}")
                    return@runCatching emptyList()
                }
                val body = response.body?.string() ?: return@runCatching emptyList()
                val root = JSONObject(body)
                val content = root.optJSONArray("content") ?: return@runCatching emptyList()

                val candidates = mutableListOf<ReccoBeatsCandidate>()
                for (i in 0 until content.length()) {
                    val item = content.getJSONObject(i)
                    val id = item.optString("id")
                    val title = item.optString("trackTitle")
                    val artistsArray = item.optJSONArray("artists")
                    val artistName = artistsArray?.optJSONObject(0)?.optString("name").orEmpty()
                    val href = item.optString("href")
                    val candidateMimikyuId = href.substringAfterLast("/").takeIf { it.isNotBlank() }

                    if (title.isNotBlank() && artistName.isNotBlank()) {
                        candidates.add(
                            ReccoBeatsCandidate(
                                id = id,
                                title = title,
                                artist = artistName,
                                mimikyuId = candidateMimikyuId,
                                durationMs = item.optLong("durationMs", 0L),
                                href = href,
                            )
                        )
                    }
                }

                recommendationsCache.put(cacheKey, candidates)
                candidates
            }
        }.onFailure {
            AppLogger.d(TAG, "ReccoBeats recommendation query failed: ${it.message}")
        }.getOrDefault(emptyList())
    }
}
