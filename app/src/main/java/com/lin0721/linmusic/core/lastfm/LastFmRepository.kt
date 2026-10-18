package com.lin0721.linmusic.core.lastfm

import android.util.LruCache
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

data class LastFmTrackInfo(
    val name: String,
    val artist: String,
    val listeners: Long = 0L,
    val playcount: Long = 0L,
    val userPlaycount: Long = 0L
)

data class LastFmArtistInfo(
    val name: String,
    val listeners: Long = 0L,
    val playcount: Long = 0L,
    val bioSummary: String = "",
    val topTag: String = "",
    val tags: List<String> = emptyList()
)

/**
 * Last.fm API client and repository.
 * Provides authentic, aggregated listener and playcount stats from millions of music scrobblers worldwide.
 * Used for:
 * 1. Ranking songs by true global listener numbers.
 * 2. Cross-checking releases to filter out spam uploads (genuine mainstream releases have real Last.fm listeners; fake spam doesn't exist).
 * 3. Providing genuine total listeners count for artist profiles.
 */
class LastFmRepository {

    companion object {
        private const val TAG = "LastFmRepository"
        private const val BASE_URL = "https://ws.audioscrobbler.com/2.0/"
        private const val DEFAULT_API_KEY = "2dc3914abf35f0d9c92d97d8f8e42b43"
        private const val USER_AGENT = "OctoberApp/1.0 (contact@october.music)"
        private const val MIN_REQUEST_INTERVAL_MS = 220L
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(7, TimeUnit.SECONDS)
        .build()

    // Request pacing mutex to stay well below Last.fm's 5 req/sec limit
    private val rateLimitMutex = kotlinx.coroutines.sync.Mutex()
    private var lastRequestTimeMs = 0L

    private suspend fun throttle() {
        rateLimitMutex.lock()
        try {
            val now = System.currentTimeMillis()
            val diff = now - lastRequestTimeMs
            if (diff < MIN_REQUEST_INTERVAL_MS) {
                kotlinx.coroutines.delay(MIN_REQUEST_INTERVAL_MS - diff)
            }
            lastRequestTimeMs = System.currentTimeMillis()
        } finally {
            rateLimitMutex.unlock()
        }
    }

    // In-memory cache for track info: "artist:track" -> LastFmTrackInfo
    private val trackCache = LruCache<String, LastFmTrackInfo>(500)

    // In-memory cache for artist info: "artist" -> LastFmArtistInfo
    private val artistCache = LruCache<String, LastFmArtistInfo>(300)

    suspend fun getTrackInfo(artist: String, track: String): LastFmTrackInfo? = withContext(Dispatchers.IO) {
        val cleanArtist = artist.trim()
        val cleanTrack = track.trim()
        if (cleanArtist.isBlank() || cleanTrack.isBlank()) return@withContext null

        val cacheKey = "${cleanArtist.lowercase()}:${cleanTrack.lowercase()}"
        trackCache.get(cacheKey)?.let { return@withContext it }

        throttle()

        runCatching {
            val encodedArtist = URLEncoder.encode(cleanArtist, StandardCharsets.UTF_8.name())
            val encodedTrack = URLEncoder.encode(cleanTrack, StandardCharsets.UTF_8.name())
            val url = "$BASE_URL?method=track.getInfo&artist=$encodedArtist&track=$encodedTrack&api_key=$DEFAULT_API_KEY&format=json"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            var body = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                response.body?.string() ?: return@runCatching null
            }
            var json = JSONObject(body)

            if (json.has("error") && json.optInt("error") == 29) {
                // Rate limit backoff: wait 500ms and retry once
                kotlinx.coroutines.delay(500)
                throttle()
                body = httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    response.body?.string() ?: return@runCatching null
                }
                json = JSONObject(body)
            }

            if (json.has("error")) {
                return@runCatching null
            }

            val trackObj = json.optJSONObject("track") ?: return@runCatching null
            val listeners = trackObj.optString("listeners", "0").toLongOrNull() ?: 0L
            val playcount = trackObj.optString("playcount", "0").toLongOrNull() ?: 0L
            val name = trackObj.optString("name", cleanTrack)
            val artistName = trackObj.optJSONObject("artist")?.optString("name") ?: cleanArtist

            val info = LastFmTrackInfo(
                name = name,
                artist = artistName,
                listeners = listeners,
                playcount = playcount
            )
            trackCache.put(cacheKey, info)
            info
        }.onFailure {
            AppLogger.d(TAG, "getTrackInfo failed for $cleanArtist - $cleanTrack: ${it.message}")
        }.getOrNull()
    }

    suspend fun getArtistInfo(artist: String): LastFmArtistInfo? = withContext(Dispatchers.IO) {
        val cleanArtist = artist.trim()
        if (cleanArtist.isBlank()) return@withContext null

        val cacheKey = cleanArtist.lowercase()
        artistCache.get(cacheKey)?.let { return@withContext it }

        throttle()

        runCatching {
            val encodedArtist = URLEncoder.encode(cleanArtist, StandardCharsets.UTF_8.name())
            val url = "$BASE_URL?method=artist.getinfo&artist=$encodedArtist&api_key=$DEFAULT_API_KEY&format=json"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            var body = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                response.body?.string() ?: return@runCatching null
            }
            var json = JSONObject(body)

            if (json.has("error") && json.optInt("error") == 29) {
                // Rate limit backoff: wait 500ms and retry once
                kotlinx.coroutines.delay(500)
                throttle()
                body = httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    response.body?.string() ?: return@runCatching null
                }
                json = JSONObject(body)
            }

            if (json.has("error")) {
                return@runCatching null
            }

            val artistObj = json.optJSONObject("artist") ?: return@runCatching null
            val stats = artistObj.optJSONObject("stats")
            val listeners = stats?.optString("listeners", "0")?.toLongOrNull() ?: 0L
            val playcount = stats?.optString("playcount", "0")?.toLongOrNull() ?: 0L
            val bio = artistObj.optJSONObject("bio")?.optString("summary", "") ?: ""

            // Extract tags for genre classification
            val tagList = mutableListOf<String>()
            val tagsContainer = artistObj.optJSONObject("tags")
            val tagArray = tagsContainer?.optJSONArray("tag")
            if (tagArray != null) {
                for (i in 0 until tagArray.length()) {
                    tagArray.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }?.let {
                        tagList.add(it)
                    }
                }
            } else {
                tagsContainer?.optJSONObject("tag")?.optString("name")?.takeIf { it.isNotBlank() }?.let {
                    tagList.add(it)
                }
            }
            val rawTopTag = tagList.firstOrNull() ?: ""
            val formattedTag = formatGenreTag(rawTopTag)

            val info = LastFmArtistInfo(
                name = artistObj.optString("name", cleanArtist),
                listeners = listeners,
                playcount = playcount,
                bioSummary = bio,
                topTag = formattedTag,
                tags = tagList
            )
            artistCache.put(cacheKey, info)
            info
        }.onFailure {
            AppLogger.d(TAG, "getArtistInfo failed for $cleanArtist: ${it.message}")
        }.getOrNull()
    }

    private fun formatGenreTag(tag: String): String {
        if (tag.isBlank()) return ""
        return when (tag.lowercase()) {
            "rnb", "r&b" -> "R&B"
            "hip-hop", "hip hop", "rap" -> "Hip-Hop"
            "pop" -> "Pop"
            "rock" -> "Rock"
            "electronic", "edm" -> "Electronic"
            "indie" -> "Indie"
            "soul" -> "Soul"
            "latin" -> "Latin"
            "country" -> "Country"
            "jazz" -> "Jazz"
            "classical" -> "Classical"
            "lofi", "lo-fi" -> "Lo-Fi"
            else -> tag.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.US) else it.toString() }
        }
    }

    /**
     * Cross-checks releases against Last.fm database.
     * Spam uploads and fake audio tracks do not exist in Last.fm or have 0 listeners.
     */
    suspend fun filterSpamTracks(tracks: List<Track>, minListeners: Long = 100L): List<Track> = withContext(Dispatchers.IO) {
        if (tracks.isEmpty()) return@withContext emptyList()
        tracks.filter { track ->
            val artistName = track.ar.firstOrNull()?.name.orEmpty()
            if (artistName.isBlank() || track.name.isBlank()) return@filter true
            val info = getTrackInfo(artistName, track.name)
            // If Last.fm has the track, check listener count; if not found yet (brand new), allow if artist is known
            if (info != null) {
                info.listeners >= minListeners
            } else {
                val artistInfo = getArtistInfo(artistName)
                artistInfo != null && artistInfo.listeners > 500L
            }
        }
    }

    /**
     * Discovers similar artists via Last.fm global collaborative filtering graph.
     * Used for Artist Radio stations and "More like [Artist]" recommendations.
     */
    suspend fun getSimilarArtists(artist: String, limit: Int = 10): List<String> = withContext(Dispatchers.IO) {
        val cleanArtist = artist.trim()
        if (cleanArtist.isBlank()) return@withContext emptyList()

        throttle()

        runCatching {
            val encodedArtist = URLEncoder.encode(cleanArtist, StandardCharsets.UTF_8.name())
            val url = "$BASE_URL?method=artist.getsimilar&artist=$encodedArtist&limit=$limit&api_key=$DEFAULT_API_KEY&format=json"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val body = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching emptyList()
                response.body?.string() ?: return@runCatching emptyList()
            }
            val json = JSONObject(body)
            val similarContainer = json.optJSONObject("similarartists") ?: return@runCatching emptyList()
            val artistArray = similarContainer.optJSONArray("artist")
            val result = mutableListOf<String>()
            if (artistArray != null) {
                for (i in 0 until artistArray.length()) {
                    artistArray.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }?.let {
                        result.add(it)
                    }
                }
            }
            result
        }.onFailure {
            AppLogger.d(TAG, "getSimilarArtists failed for $cleanArtist: ${it.message}")
        }.getOrDefault(emptyList())
    }

    /**
     * Gets global top artists from Last.fm charts for trending recommendations.
     */
    suspend fun getTopArtists(limit: Int = 10): List<String> = withContext(Dispatchers.IO) {
        throttle()

        runCatching {
            val url = "$BASE_URL?method=chart.gettopartists&limit=$limit&api_key=$DEFAULT_API_KEY&format=json"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val body = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching emptyList()
                response.body?.string() ?: return@runCatching emptyList()
            }
            val json = JSONObject(body)
            val artistsContainer = json.optJSONObject("artists") ?: return@runCatching emptyList()
            val artistArray = artistsContainer.optJSONArray("artist")
            val result = mutableListOf<String>()
            if (artistArray != null) {
                for (i in 0 until artistArray.length()) {
                    artistArray.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }?.let {
                        result.add(it)
                    }
                }
            }
            result
        }.onFailure {
            AppLogger.d(TAG, "getTopArtists failed: ${it.message}")
        }.getOrDefault(emptyList())
    }
}
