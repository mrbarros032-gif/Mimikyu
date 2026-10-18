package com.lin0721.linmusic.core.qobuz

import com.lin0721.linmusic.core.log.AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs

private const val TAG = "QobuzService"
private const val APP_ID = "285474720"
private const val BASE_URL = "https://www.qobuz.com/api.json/0.2"
private const val DURATION_TOLERANCE_SEC = 3

/**
 * Qobuz lossless stream resolver — used exclusively for quality upgrade.
 * Takes title + artist + duration, searches the Qobuz catalog, duration-matches,
 * and returns a FLAC stream URL if available.
 */
class QobuzService(private val httpClient: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Attempt to resolve a lossless (FLAC) stream URL for the given track metadata.
     * @return FLAC stream URL, or null if no suitable match was found.
     */
    suspend fun resolveLosslessStream(
        title: String,
        artist: String,
        durationSec: Int
    ): String? {
        return try {
            // 1. Search Qobuz catalog
            val searchUrl = "$BASE_URL/catalog/search"
            val searchResponse = httpClient.get(searchUrl) {
                parameter("query", "$title $artist")
                parameter("type", "tracks")
                parameter("limit", "5")
                parameter("app_id", APP_ID)
            }.bodyAsText()

            val searchResult = json.decodeFromString<QobuzSearchResponse>(searchResponse)
            val tracks = searchResult.tracks?.items ?: return null

            // 2. Duration-match: find best match within ±3s tolerance
            val matched = tracks.firstOrNull { track ->
                abs(track.duration - durationSec) <= DURATION_TOLERANCE_SEC &&
                    fuzzyMatch(track.title, title) &&
                    fuzzyMatch(track.performer?.name ?: "", artist)
            } ?: return null

            // 3. Request stream URL for FLAC (format_id=27 for Hi-Res, 6 for CD, 5 for 320k)
            val fileUrl = "$BASE_URL/track/getFileUrl"
            for (formatId in listOf(27, 6, 5)) {
                try {
                    val streamResponse = httpClient.get(fileUrl) {
                        parameter("track_id", matched.id.toString())
                        parameter("format_id", formatId.toString())
                        parameter("app_id", APP_ID)
                    }.bodyAsText()

                    val streamResult = json.decodeFromString<QobuzStreamResponse>(streamResponse)
                    if (!streamResult.url.isNullOrBlank()) {
                        AppLogger.i(TAG, "Qobuz lossless resolved: format=$formatId, track=${matched.title}")
                        return streamResult.url
                    }
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Qobuz format $formatId failed for track ${matched.id}", e)
                }
            }
            null
        } catch (e: Exception) {
            AppLogger.w(TAG, "Qobuz lossless resolution failed for '$title' by '$artist'", e)
            null
        }
    }

    private fun fuzzyMatch(a: String, b: String): Boolean {
        val na = a.lowercase().trim()
        val nb = b.lowercase().trim()
        return na.contains(nb) || nb.contains(na) || levenshteinSimilarity(na, nb) > 0.7
    }

    private fun levenshteinSimilarity(s1: String, s2: String): Double {
        if (s1.isEmpty() && s2.isEmpty()) return 1.0
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1.0
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return 1.0 - dp[s1.length][s2.length].toDouble() / maxLen
    }
}

// ---- Qobuz API response models ----

@Serializable
data class QobuzSearchResponse(
    val tracks: QobuzTracksResult? = null
)

@Serializable
data class QobuzTracksResult(
    val items: List<QobuzTrack> = emptyList()
)

@Serializable
data class QobuzTrack(
    val id: Long = 0,
    val title: String = "",
    val duration: Int = 0,
    val performer: QobuzPerformer? = null
)

@Serializable
data class QobuzPerformer(
    val name: String = ""
)

@Serializable
data class QobuzStreamResponse(
    val url: String? = null,
    val format_id: Int? = null,
    val mime_type: String? = null
)
