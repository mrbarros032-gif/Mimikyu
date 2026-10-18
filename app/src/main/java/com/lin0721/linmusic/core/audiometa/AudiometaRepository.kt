package com.lin0721.linmusic.core.audiometa

import android.util.LruCache
import com.lin0721.linmusic.BuildConfig
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.metadata.cleanMetadataText
import com.lin0721.linmusic.core.preferences.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

data class AudiometaArtistInfo(
    val id: String,
    val name: String,
    val bio: String? = null,
    val genres: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    val mimikyuMonthlyListeners: Long? = null,
    val mimikyuFollowers: Long? = null,
    val youtubeSubscribers: Long? = null,
    val followersTotal: Long? = null
)

data class AudiometaTrack(
    val id: String,
    val name: String,
    val popularity: Int = 0,
    val tempo: Float? = null
)

/**
 * Repository for interacting with the audiometa.io catalog REST API.
 * Rate limit: max 100 requests per 60 seconds per key.
 */
class AudiometaRepository(
    private val settingsPreferences: SettingsPreferences? = null,
    private val customApiKey: String? = null
) {
    companion object {
        private const val TAG = "AudiometaRepository"
        private const val BASE_URL = "https://api.audiometa.io/v1"
        // 100 req/60s rate limit => minimum interval 600ms between calls
        private const val MIN_REQUEST_INTERVAL_MS = 600L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var cachedApiKey: String = customApiKey ?: ""

    init {
        if (cachedApiKey.isBlank()) {
            val env = System.getenv("AUDIOMETA_API_KEY")
            if (!env.isNullOrBlank()) {
                cachedApiKey = env.trim()
            }
        }
        // Listen to reactive settings updates if available
        settingsPreferences?.let { prefs ->
            scope.launch {
                prefs.audiometaApiKey.collect { key ->
                    if (key.isNotBlank()) {
                        cachedApiKey = key.trim()
                    }
                }
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    private val rateLimitMutex = Mutex()
    private var lastRequestTimeMs = 0L

    private val artistCache = LruCache<String, AudiometaArtistInfo>(300)

    fun getApiKey(): String = cachedApiKey

    fun setApiKey(key: String) {
        cachedApiKey = key.trim()
    }

    private suspend fun throttle() {
        rateLimitMutex.lock()
        try {
            val now = System.currentTimeMillis()
            val diff = now - lastRequestTimeMs
            if (diff < MIN_REQUEST_INTERVAL_MS) {
                delay(MIN_REQUEST_INTERVAL_MS - diff)
            }
            lastRequestTimeMs = System.currentTimeMillis()
        } finally {
            rateLimitMutex.unlock()
        }
    }

    /**
     * Resolves artist info by search query, fetching full details from /artists/{id}.
     */
    suspend fun getArtistInfo(artistName: String): AudiometaArtistInfo? = withContext(Dispatchers.IO) {
        val cleanName = cleanMetadataText(artistName).trim()
        if (cleanName.isBlank()) return@withContext null

        val cacheKey = cleanName.lowercase()
        artistCache.get(cacheKey)?.let { return@withContext it }

        val apiKey = cachedApiKey
        if (apiKey.isBlank()) {
            AppLogger.d(TAG, "Audiometa API key not configured; skipping request for '$cleanName'")
            return@withContext null
        }

        runCatching {
            // Step 1: Search for artist to resolve audiometa ID (amart_...)
            val encodedQuery = URLEncoder.encode(cleanName, StandardCharsets.UTF_8.name())
            val searchUrl = "$BASE_URL/search?q=$encodedQuery&type=artist&limit=1"

            val searchBody = executeWithRetry(searchUrl, apiKey) ?: return@runCatching null
            val searchJson = JSONObject(searchBody)
            val artistsObj = searchJson.optJSONObject("artists") ?: return@runCatching null
            val items = artistsObj.optJSONArray("items") ?: return@runCatching null
            if (items.length() == 0) {
                AppLogger.d(TAG, "No artist found on Audiometa for '$cleanName'")
                return@runCatching null
            }

            val firstItem = items.getJSONObject(0)
            val artistId = firstItem.optString("id")
            if (artistId.isBlank()) return@runCatching null

            // Step 2: Fetch full artist object from /artists/{id}
            getArtistById(artistId, cachedArtistKey = cacheKey)
        }.onFailure { e ->
            AppLogger.w(TAG, "getArtistInfo failed for '$cleanName': ${e.message}")
        }.getOrNull()
    }

    /**
     * Fetches full artist entity by durable audiometa.io ID (e.g. amart_8RELWiHB7RZOqrzk).
     */
    suspend fun getArtistById(artistId: String, cachedArtistKey: String? = null): AudiometaArtistInfo? = withContext(Dispatchers.IO) {
        val apiKey = cachedApiKey
        if (apiKey.isBlank() || artistId.isBlank()) return@withContext null

        artistCache.get(artistId.lowercase())?.let { return@withContext it }

        runCatching {
            val url = "$BASE_URL/artists/$artistId"
            val body = executeWithRetry(url, apiKey) ?: return@runCatching null
            val json = JSONObject(body)

            val name = json.optString("name", "")
            val bio = json.optString("bio", "").takeIf { it.isNotBlank() }
            val genres = mutableListOf<String>()
            json.optJSONArray("genres")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val g = arr.optString(i)
                    if (g.isNotBlank()) genres.add(g)
                }
            }

            val images = mutableListOf<String>()
            json.optJSONArray("images")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val imgObj = arr.optJSONObject(i)
                    val u = imgObj?.optString("url")
                    if (!u.isNullOrBlank()) images.add(u)
                }
            }

            val followersObj = json.optJSONObject("followers")
            val followersTotal = followersObj?.optLong("total")?.takeIf { it > 0L }

            val statsObj = json.optJSONObject("stats")
            val mimikyuMonthly = statsObj?.optLong("mimikyu_monthly_listeners")?.takeIf { it > 0L }
            val mimikyuFollowers = statsObj?.optLong("mimikyu_followers")?.takeIf { it > 0L }
            val youtubeSubs = statsObj?.optLong("youtube_subscribers")?.takeIf { it > 0L }

            val info = AudiometaArtistInfo(
                id = artistId,
                name = name,
                bio = bio,
                genres = genres,
                images = images,
                mimikyuMonthlyListeners = mimikyuMonthly,
                mimikyuFollowers = mimikyuFollowers,
                youtubeSubscribers = youtubeSubs,
                followersTotal = followersTotal
            )

            artistCache.put(artistId.lowercase(), info)
            if (!cachedArtistKey.isNullOrBlank()) {
                artistCache.put(cachedArtistKey.lowercase(), info)
            }
            if (name.isNotBlank()) {
                artistCache.put(name.lowercase(), info)
            }

            AppLogger.i(TAG, "[Audiometa] artist '$name' id=$artistId monthlyListeners=$mimikyuMonthly")
            info
        }.onFailure { e ->
            AppLogger.w(TAG, "getArtistById failed for '$artistId': ${e.message}")
        }.getOrNull()
    }

    private suspend fun executeWithRetry(url: String, apiKey: String): String? {
        throttle()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .header("User-Agent", "OctoberApp/1.0 (contact@october.music)")
            .build()

        var response = httpClient.newCall(request).execute()
        var code = response.code
        var bodyStr = response.body?.string()
        response.close()

        if (code == 429) {
            // Rate limit encountered: backoff 2 seconds and retry once
            AppLogger.w(TAG, "[Audiometa] HTTP 429 rate limit exceeded; backing off 2s before retry")
            delay(2000L)
            throttle()
            response = httpClient.newCall(request).execute()
            code = response.code
            bodyStr = response.body?.string()
            response.close()
        }

        if (code !in 200..299 || bodyStr.isNullOrBlank()) {
            if (code == 401) {
                AppLogger.w(TAG, "[Audiometa] HTTP 401: Invalid or missing API key")
            } else {
                AppLogger.w(TAG, "[Audiometa] HTTP $code from $url: $bodyStr")
            }
            return null
        }

        return bodyStr
    }
    suspend fun getArtistGenres(artistName: String): List<String> {
        return getArtistInfo(artistName)?.genres.orEmpty()
    }

    suspend fun getArtistTopTracks(artistId: String): List<AudiometaTrack> = withContext(Dispatchers.IO) {
        val apiKey = cachedApiKey
        if (apiKey.isBlank() || artistId.isBlank()) return@withContext emptyList()

        runCatching {
            val url = "$BASE_URL/artists/$artistId/top-tracks"
            val body = executeWithRetry(url, apiKey) ?: return@runCatching emptyList()
            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: return@runCatching emptyList()
            (0 until items.length()).mapNotNull { i ->
                val obj = items.optJSONObject(i) ?: return@mapNotNull null
                val id = obj.optString("id")
                val name = obj.optString("name")
                if (id.isBlank() || name.isBlank()) null
                else AudiometaTrack(
                    id = id,
                    name = name,
                    popularity = obj.optInt("popularity", 0),
                    tempo = obj.optJSONObject("audio_features")?.optDouble("tempo")?.toFloat()
                )
            }
        }.getOrDefault(emptyList())
    }

}
