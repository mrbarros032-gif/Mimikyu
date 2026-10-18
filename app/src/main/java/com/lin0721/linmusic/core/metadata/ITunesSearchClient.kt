package com.lin0721.linmusic.core.metadata

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@Serializable
data class ITunesSearchResult(
    val resultCount: Int = 0,
    val results: List<ITunesTrack> = emptyList()
)

@Serializable
data class ITunesTrack(
    val trackId: Long = 0,
    val artistName: String = "",
    val trackName: String = "",
    val collectionName: String? = null,
    val artworkUrl100: String? = null,
    val releaseDate: String? = null,
    val primaryGenreName: String? = null,
    val trackTimeMillis: Long? = null,
    val wrapperType: String? = null,
    val kind: String? = null,
    val previewUrl: String? = null
) {
    /**
     * Upgrades artwork URL to high resolution (default 1200x1200).
     * Replaces e.g. 100x100bb.jpg or 60x60.jpg with 1200x1200bb.jpg.
     */
    fun getUpgradedArtworkUrl(size: Int = 1200): String? {
        val original = artworkUrl100 ?: return null
        return if (Regex("""\d+x\d+bb""").containsMatchIn(original)) {
            original.replace(Regex("""\d+x\d+bb"""), "${size}x${size}bb")
        } else if (Regex("""\d+x\d+""").containsMatchIn(original)) {
            original.replace(Regex("""\d+x\d+"""), "${size}x${size}bb")
        } else {
            original
        }
    }
}

class ITunesSearchClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun searchSongs(query: String, limit: Int = 5): List<ITunesTrack> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encodedTerm = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (e: Exception) {
            query.replace(" ", "+")
        }
        val url = "https://itunes.apple.com/search?term=$encodedTerm&media=music&entity=song&limit=$limit"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "October/1.0 (Android)")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.tag("ITunesSearchClient").w("iTunes search failed with HTTP ${response.code}")
                    return@withContext emptyList()
                }
                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val parsed = json.decodeFromString<ITunesSearchResult>(bodyString)
                parsed.results.filter { it.wrapperType == null || it.wrapperType == "track" }
            }
        } catch (e: Exception) {
            Timber.tag("ITunesSearchClient").w("iTunes search network error for query='$query': ${e.message}")
            emptyList()
        }
    }

    /**
     * Search songs by title and artist, with automatic fallback to title-only if needed.
     */
    suspend fun searchSongsByTitleAndArtist(title: String, artist: String, limit: Int = 5): List<ITunesTrack> {
        val combinedQuery = "$title $artist".trim()
        val firstAttempt = searchSongs(combinedQuery, limit)
        if (firstAttempt.isNotEmpty()) return firstAttempt

        // If combined search returned no results, search by title alone (caller will verify artist match)
        if (title.isNotBlank()) {
            return searchSongs(title, limit)
        }
        return emptyList()
    }
}
