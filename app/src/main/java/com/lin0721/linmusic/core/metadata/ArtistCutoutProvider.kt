package com.lin0721.linmusic.core.metadata

import com.lin0721.linmusic.core.log.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private const val TAG = "ArtistCutoutProvider"

@Serializable
private data class AudioDbResponse(
    val artists: List<AudioDbArtist>? = null
)

@Serializable
private data class AudioDbArtist(
    val strArtist: String? = null,
    val strArtistCutout: String? = null,
    val strArtistThumb: String? = null
)

/**
 * Resolves transparent cutout PNGs of artists for Mimikyu "THIS IS" & "Made For Your Vibe" cards.
 * Uses TheAudioDB public API (strArtistCutout) with preloaded fast fallbacks and caching.
 */
object ArtistCutoutProvider {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // Preloaded high-quality transparent artist cutouts (from TheAudioDB & Fanart.tv CDN)
    private val PRELOADED_CUTOUTS = mapOf(
        "taylor swift" to "https://www.theaudiodb.com/images/media/artist/cutout/taylor-swift-4e36d4b4ec0f7.png",
        "drake" to "https://www.theaudiodb.com/images/media/artist/cutout/uqxyqp1433245452.png",
        "the weeknd" to "https://www.theaudiodb.com/images/media/artist/cutout/the-weeknd-51bb080155b41.png",
        "kanye west" to "https://www.theaudiodb.com/images/media/artist/cutout/kanye-west-4e36d3910c2c1.png",
        "kendrick lamar" to "https://www.theaudiodb.com/images/media/artist/cutout/kendrick-lamar-5103c80a84e62.png",
        "billie eilish" to "https://www.theaudiodb.com/images/media/artist/cutout/billie-eilish-5c935ee5723b4.png",
        "bruno mars" to "https://www.theaudiodb.com/images/media/artist/cutout/bruno-mars-4e36d0b679b36.png",
        "dua lipa" to "https://www.theaudiodb.com/images/media/artist/cutout/dua-lipa-59367dcaeb894.png",
        "sza" to "https://www.theaudiodb.com/images/media/artist/cutout/sza-594236a928e19.png",
        "lana del rey" to "https://www.theaudiodb.com/images/media/artist/cutout/lana-del-rey-4f15d2a938c4b.png",
        "michael jackson" to "https://www.theaudiodb.com/images/media/artist/cutout/michael-jackson-4e36d3f2ec2b5.png",
        "ariana grande" to "https://www.theaudiodb.com/images/media/artist/cutout/ariana-grande-522db9ffc5d8a.png",
        "rihanna" to "https://www.theaudiodb.com/images/media/artist/cutout/rihanna-4e36d46d0a7a4.png",
        "ed sheeran" to "https://www.theaudiodb.com/images/media/artist/cutout/ed-sheeran-4f36a5cf8be05.png",
        "coldplay" to "https://www.theaudiodb.com/images/media/artist/cutout/coldplay-4e36d10c85c26.png",
        "post malone" to "https://www.theaudiodb.com/images/media/artist/cutout/post-malone-594a11f269a8b.png",
        "eminem" to "https://www.theaudiodb.com/images/media/artist/cutout/eminem-4e36d1f67f331.png",
        "justin bieber" to "https://www.theaudiodb.com/images/media/artist/cutout/justin-bieber-4e36d37617b43.png"
    )

    private val cache = ConcurrentHashMap<String, String>()

    init {
        cache.putAll(PRELOADED_CUTOUTS)
    }

    /**
     * Resolves a transparent artist cutout PNG URL.
     * Checks in-memory cache, queries TheAudioDB public API, or falls back to standard artist portrait.
     */
    suspend fun getArtistCutout(artistName: String): String = withContext(Dispatchers.IO) {
        if (artistName.isBlank()) return@withContext ""
        val clean = artistName.lowercase().trim()

        val cached = getCachedCutout(clean)
        if (cached.isNotBlank()) return@withContext cached

        // Query TheAudioDB for transparent cutout
        try {
            val encoded = URLEncoder.encode(clean, "UTF-8")
            val url = "https://www.theaudiodb.com/api/v1/json/2/search.php?s=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mimikyu/1.0 (Android)")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val parsed = json.decodeFromString<AudioDbResponse>(bodyString)
                        val artist = parsed.artists?.firstOrNull()
                        val cutoutUrl = artist?.strArtistCutout?.takeIf { it.isNotBlank() }
                        if (cutoutUrl != null) {
                            cache[clean] = cutoutUrl
                            return@withContext cutoutUrl
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "TheAudioDB cutout search failed for '$artistName': ${e.message}")
        }

        // Fallback to high-res artist portrait
        val portrait = ArtistPortraitProvider.getCachedPortrait(clean)
        if (portrait.isNotBlank()) {
            cache[clean] = portrait
            return@withContext portrait
        }

        ""
    }

    /**
     * Returns cached transparent cutout or portrait immediately.
     */
    fun getCachedCutout(artistName: String): String {
        if (artistName.isBlank()) return ""
        val clean = artistName.lowercase().trim()
        val direct = cache[clean]
        if (direct != null) return direct

        // Partial match
        val partial = cache.entries.firstOrNull {
            it.key.length > 2 && (clean.contains(it.key) || it.key.contains(clean))
        }?.value
        if (partial != null) return partial

        return ArtistPortraitProvider.getCachedPortrait(clean)
    }
}
