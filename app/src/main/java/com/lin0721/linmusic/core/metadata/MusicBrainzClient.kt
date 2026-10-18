package com.lin0721.linmusic.core.metadata

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
internal data class MbRecordingResponse(
    val recordings: List<MbRecording> = emptyList()
)

@Serializable
internal data class MbRecording(
    val id: String = "",
    val title: String = "",
    val length: Long? = null,
    @SerialName("artist-credit")
    val artistCredit: List<MbArtistCredit> = emptyList(),
    val releases: List<MbReleaseRef> = emptyList()
)

@Serializable
internal data class MbReleaseResponse(
    val releases: List<MbRelease> = emptyList()
)

@Serializable
internal data class MbRelease(
    val id: String = "",
    val title: String = "",
    val status: String? = null,
    @SerialName("artist-credit")
    val artistCredit: List<MbArtistCredit> = emptyList()
)

@Serializable
internal data class MbArtistCredit(
    val name: String = ""
)

@Serializable
internal data class MbReleaseRef(
    val id: String = "",
    val title: String = "",
    val status: String? = null,
    val date: String? = null
)

@Serializable
internal data class CaaResponse(
    val images: List<CaaImage> = emptyList(),
    val release: String? = null
)

@Serializable
internal data class CaaImage(
    val id: Long = 0,
    val image: String = "",
    val front: Boolean = false,
    val thumbnails: Map<String, String> = emptyMap()
)

data class MusicBrainzMatch(
    val releaseMbid: String? = null,
    val title: String = "",
    val artist: String = "",
    val album: String? = null,
    val releaseDate: String? = null,
    val artworkUrl: String? = null
)

/**
 * MusicBrainz & Cover Art Archive client.
 * Provides fallback album art when iTunes has no match.
 *
 * Enforces MusicBrainz's 1 request/second rate-limiting policy and includes
 * the required application User-Agent header.
 */
class MusicBrainzClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val rateLimitMutex = Mutex()
    private var lastMbRequestTime = 0L

    companion object {
        private const val TAG = "MusicBrainzClient"
        private const val USER_AGENT = "October/1.0.0 ( https://github.com/lin0721/linmusic )"
        private const val MB_BASE = "https://musicbrainz.org/ws/2"
        private const val CAA_BASE = "https://coverartarchive.org"
    }

    /**
     * Throttles requests to MusicBrainz to at most 1 request per second.
     */
    private suspend fun throttleMusicBrainz() {
        rateLimitMutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastMbRequestTime
            if (elapsed < 1000L) {
                delay(1000L - elapsed)
            }
            lastMbRequestTime = System.currentTimeMillis()
        }
    }

    private fun sanitizeForLucene(input: String): String {
        return input.replace(Regex("""[+\-!(){}\[\]^"~*?:\\/]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /**
     * Searches MusicBrainz for a release MBID matching the given song or album.
     */
    suspend fun findReleaseMbid(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long? = null
    ): String? {
        return resolveVerifiedTrack(title, artist, album, durationMs)?.releaseMbid
    }

    /**
     * Searches MusicBrainz and Cover Art Archive to verify the track against metadata
     * and fetch high-resolution cover art along with verified track/album data.
     */
    suspend fun resolveVerifiedTrack(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long? = null
    ): MusicBrainzMatch? = withContext(Dispatchers.IO) {
        val cleanArtist = sanitizeForLucene(artist)
        val cleanTitle = sanitizeForLucene(title)
        val cleanAlbum = album?.let { sanitizeForLucene(it) }

        // 1. If album is specified, try direct release search
        if (!cleanAlbum.isNullOrBlank() && cleanArtist.isNotBlank()) {
            val albumMatch = searchReleaseByAlbumWithMetadata(cleanAlbum, cleanArtist)
            if (albumMatch != null) return@withContext albumMatch
        }

        // 2. Search recordings by song title and artist
        if (cleanTitle.isNotBlank() && cleanArtist.isNotBlank()) {
            return@withContext searchRecordingWithMetadata(title, artist, durationMs)
        }

        null
    }

    private suspend fun searchReleaseByAlbumWithMetadata(album: String, artist: String): MusicBrainzMatch? {
        throttleMusicBrainz()
        val query = "release:\"$album\" AND artist:\"$artist\""
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$MB_BASE/release/?query=$encodedQuery&fmt=json&limit=5"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val result = json.decodeFromString<MbReleaseResponse>(body)

                for (candidate in result.releases) {
                    val candidateArtist = candidate.artistCredit.joinToString(" ") { it.name }
                    val confidence = FuzzyMatcher.matchConfidence(
                        queryTitle = album,
                        queryArtist = artist,
                        candidateTitle = candidate.title,
                        candidateArtist = candidateArtist
                    )
                    if (confidence >= 0.70) {
                        val art = getCoverArt(candidate.id)
                        return MusicBrainzMatch(
                            releaseMbid = candidate.id,
                            title = "",
                            artist = candidateArtist,
                            album = candidate.title,
                            artworkUrl = art
                        )
                    }
                }
                null
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w("MB release search error: ${e.message}")
            null
        }
    }

    private suspend fun searchRecordingWithMetadata(
        title: String,
        artist: String,
        durationMs: Long?
    ): MusicBrainzMatch? {
        throttleMusicBrainz()
        val cleanTitle = sanitizeForLucene(title)
        val cleanArtist = sanitizeForLucene(artist)
        val query = "recording:\"$cleanTitle\" AND artist:\"$cleanArtist\""
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$MB_BASE/recording/?query=$encodedQuery&fmt=json&limit=5"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val result = json.decodeFromString<MbRecordingResponse>(body)

                var bestRecording: MbRecording? = null
                var highestConfidence = 0.0

                for (rec in result.recordings) {
                    val candidateArtist = rec.artistCredit.joinToString(" ") { it.name }
                    val confidence = FuzzyMatcher.matchConfidence(
                        queryTitle = title,
                        queryArtist = artist,
                        candidateTitle = rec.title,
                        candidateArtist = candidateArtist,
                        queryDurationMs = durationMs,
                        candidateDurationMs = rec.length
                    )
                    if (confidence > highestConfidence) {
                        highestConfidence = confidence
                        bestRecording = rec
                    }
                }

                if (bestRecording != null && FuzzyMatcher.isConfidentMatch(highestConfidence)) {
                    val candArtist = bestRecording.artistCredit.joinToString(" ") { it.name }.ifBlank { artist }

                    // Sort releases: Official first, then any other
                    val releases = bestRecording.releases.sortedByDescending {
                        if (it.status.equals("Official", ignoreCase = true)) 1 else 0
                    }

                    var chosenRelease: MbReleaseRef? = null
                    var coverArtUrl: String? = null

                    // Check up to 3 candidate releases for Cover Art Archive scans
                    for (rel in releases.take(3)) {
                        val art = getCoverArt(rel.id)
                        if (!art.isNullOrBlank()) {
                            chosenRelease = rel
                            coverArtUrl = art
                            break
                        }
                    }

                    if (chosenRelease == null) {
                        chosenRelease = releases.firstOrNull()
                    }

                    MusicBrainzMatch(
                        releaseMbid = chosenRelease?.id,
                        title = bestRecording.title,
                        artist = candArtist,
                        album = chosenRelease?.title,
                        releaseDate = chosenRelease?.date,
                        artworkUrl = coverArtUrl
                    )
                } else null
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w("MB recording search error: ${e.message}")
            null
        }
    }

    /**
     * Queries Cover Art Archive for high-resolution front artwork (1200x1200 or original).
     * Returns null if no cover art is found.
     */
    suspend fun getCoverArt(releaseMbid: String): String? = withContext(Dispatchers.IO) {
        if (releaseMbid.isBlank()) return@withContext null
        val url = "$CAA_BASE/release/$releaseMbid"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.tag(TAG).d("Cover Art Archive HTTP ${response.code} for $releaseMbid")
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                val caa = json.decodeFromString<CaaResponse>(body)

                val front = caa.images.firstOrNull { it.front } ?: caa.images.firstOrNull()
                    ?: return@withContext null

                // Resolution priority: 1200 thumbnail -> large (500) -> full-size original image
                val artworkUrl = front.thumbnails["1200"]
                    ?: front.thumbnails["large"]
                    ?: front.image

                artworkUrl.takeIf { it.isNotBlank() }?.let { rawUrl ->
                    if (rawUrl.startsWith("http://")) rawUrl.replace("http://", "https://") else rawUrl
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).d("Cover Art Archive lookup failed for $releaseMbid: ${e.message}")
            null
        }
    }
}
