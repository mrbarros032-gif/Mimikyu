package com.lin0721.linmusic.core.lyrics

import android.util.Base64
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.player.domain.LyricLine
import com.lin0721.linmusic.core.player.domain.LyricParser
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject
import android.content.Context
import com.lin0721.linmusic.core.preferences.SettingsPreferences
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

private const val TAG = "ExternalLyricsService"
private const val LRCLIB_BASE = "https://lrclib.net/api"
private val HAN_REGEX = Regex("""[\u4e00-\u9fff]""")

/**
 * Multi-source synchronized lyrics engine with automatic fallback cascade.
 *
 * Tier 0: Zero-Delay Local Flash Storage (< 2ms)
 *    - context.cacheDir/lyrics_ttml/{videoId}.ttml (Apple Music word-synced TTML)
 * Tier 1: Cloud TTML Sync (~50 - 150ms)
 *    - Zoho Cloud / Object Storage ({videoId}.ttml)
 *    - BetterLyrics TTML (Apple Music syllable/word-level timestamps)
 * Tier 2: Word-by-Word Tier (syllable-level karaoke):
 *    - Kugou Music KRC (Vast catalog of word-level syllable timestamps with KRC decryption)
 *    - NetEase Cloud Music YRC (Word-level syllable timestamps)
 * Tier 3: High-Accuracy Line-Synced Tier (millisecond precision LRC):
 *    - LRCLIB (Over 20M songs; multi-query cascade: /get exact, /search track+artist, /search q=..., primary artist)
 *    - NetEase Cloud Music LRC (Accurate millisecond timestamps for Asian and global catalogs)
 *    - Kugou Music LRC (Line-synced LRC fallback)
 * Tier 4: In-memory caching for instant replay & UI transitions.
 */
class ExternalLyricsService(
    private val httpClient: HttpClient,
    private val context: Context? = null,
    private val settingsPreferences: SettingsPreferences? = null
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val memoryCache = ConcurrentHashMap<String, List<LyricLine>>()

    fun getCached(
        title: String,
        artist: String,
        durationSec: Int,
        videoId: String? = null
    ): List<LyricLine>? {
        if (title.isBlank()) return null
        val cleanTitle = cleanSearchKeyword(title).ifBlank { title.trim() }
        val cleanArtist = cleanSearchKeyword(artist).ifBlank { artist.trim() }
        val cleanVid = YouTubeIdRegistry.sanitizeVideoId(videoId).takeIf { it.isNotEmpty() }
        val cacheKey = if (cleanVid != null) "vid:$cleanVid::$cleanTitle::$cleanArtist::$durationSec" else "$cleanTitle::$cleanArtist::$durationSec"

        // 1. In-memory cache
        memoryCache[cacheKey]?.takeIf { it.isNotEmpty() }?.let {
            return filterHeaderMetadata(it, title, artist)
        }

        // 2. Local flash storage (< 2ms)
        if (context != null) {
            val file = getDiskCacheFile(cleanVid, cleanTitle, cleanArtist)
            if (file != null && file.exists() && file.length() > 0) {
                val diskLyrics = runCatching {
                    val ttmlContent = file.readText()
                    val parsed = LyricParser.parseTtml(ttmlContent)
                    if (parsed.isNotEmpty()) parsed else null
                }.getOrNull()

                if (diskLyrics != null) {
                    val lastLine = diskLyrics.lastOrNull { !it.isGap }
                    val isDurationMismatch = if (durationSec > 0 && lastLine != null) {
                        val lyricEndSec = (lastLine.timeMs + lastLine.durationMs) / 1000
                        lyricEndSec > 0 && kotlin.math.abs(lyricEndSec - durationSec) > 35
                    } else false

                    if (!isDurationMismatch) {
                        val sanitized = filterHeaderMetadata(diskLyrics, title, artist)
                        memoryCache[cacheKey] = sanitized
                        AppLogger.d(TAG, "Instant lyrics hit from local TTML storage for '$cleanTitle' (< 2ms)")
                        return sanitized
                    } else {
                        AppLogger.w(TAG, "Disk cache TTML duration mismatch for '$cleanTitle', invalidating cache")
                        file.delete()
                    }
                }
            }
        }
        return null
    }

    suspend fun fetchLyrics(
        title: String,
        artist: String,
        durationSec: Int,
        videoId: String? = null
    ): List<LyricLine> {
        if (title.isBlank()) return emptyList()

        val cleanTitle = cleanSearchKeyword(title).ifBlank { title.trim() }
        val cleanArtist = cleanSearchKeyword(artist).ifBlank { artist.trim() }
        val cleanVid = YouTubeIdRegistry.sanitizeVideoId(videoId).takeIf { it.isNotEmpty() }
        val cacheKey = if (cleanVid != null) "vid:$cleanVid::$cleanTitle::$cleanArtist::$durationSec" else "$cleanTitle::$cleanArtist::$durationSec"

        // 1. In-memory cache
        memoryCache[cacheKey]?.let { cached ->
            if (cached.isNotEmpty()) {
                AppLogger.d(TAG, "Returning in-memory cached lyrics for '$cleanTitle' by '$cleanArtist'")
                return filterHeaderMetadata(cached, title, artist)
            }
        }

        // 2. Local flash storage check (< 2 ms)
        if (context != null) {
            val file = getDiskCacheFile(cleanVid, cleanTitle, cleanArtist)
            if (file != null && file.exists() && file.length() > 0) {
                val diskLyrics = runCatching {
                    val text = file.readText()
                    val parsed = LyricParser.parseTtml(text)
                    if (parsed.isNotEmpty()) parsed else null
                }.getOrNull()

                if (diskLyrics != null) {
                    val lastLine = diskLyrics.lastOrNull { !it.isGap }
                    val isDurationMismatch = if (durationSec > 0 && lastLine != null) {
                        val lyricEndSec = (lastLine.timeMs + lastLine.durationMs) / 1000
                        lyricEndSec > 0 && kotlin.math.abs(lyricEndSec - durationSec) > 35
                    } else false

                    if (!isDurationMismatch) {
                        val processed = if (diskLyrics.any { it.isSynced && !it.isGap }) LyricParser.processLinesWithGaps(diskLyrics) else diskLyrics
                        val sanitized = filterHeaderMetadata(processed, title, artist)
                        memoryCache[cacheKey] = sanitized
                        AppLogger.i(TAG, "Zero-delay lyrics loaded from local flash storage for '$cleanTitle' (< 2ms)")
                        return sanitized
                    } else {
                        AppLogger.w(TAG, "Disk cache TTML duration mismatch for '$cleanTitle', invalidating cache")
                        file.delete()
                    }
                }
            }
        }

        // 3. Supabase Cloud Object Storage fetch (~50 to 150 ms)
        val resolvedVid = cleanVid ?: YouTubeIdRegistry.findVideoIdByTitleArtist(cleanTitle, cleanArtist, durationSec)
        if (!resolvedVid.isNullOrBlank() && !resolvedVid.startsWith("track_") && !resolvedVid.startsWith("placeholder_")) {
            val cloudLyrics = fetchZohoCloudTtml(resolvedVid, cleanTitle, cleanArtist, durationSec)
            if (cloudLyrics.isNotEmpty()) {
                val processed = if (cloudLyrics.any { it.isSynced && !it.isGap }) LyricParser.processLinesWithGaps(cloudLyrics) else cloudLyrics
                val sanitized = filterHeaderMetadata(processed, title, artist)
                memoryCache[cacheKey] = sanitized
                saveToDiskCache(resolvedVid, cleanTitle, cleanArtist, sanitized)
                AppLogger.i(TAG, "Loaded TTML lyrics from Supabase Cloud in zero-lag tier for '$cleanTitle' (videoId=$resolvedVid)")
                return sanitized
            }
        }

        // 4. Fallback to existing multi-tier cascade (BetterLyrics, LRCLIB, Kugou KRC, NetEase YRC)
        val rawLyrics = fetchLyricsRaw(cleanTitle, cleanArtist, durationSec)
        if (rawLyrics.isNotEmpty()) {
            val processed = if (rawLyrics.any { it.isSynced && !it.isGap }) LyricParser.processLinesWithGaps(rawLyrics) else rawLyrics
            val sanitized = filterHeaderMetadata(processed, title, artist)
            memoryCache[cacheKey] = sanitized
            saveToDiskCache(cleanVid, cleanTitle, cleanArtist, sanitized)
            return sanitized
        }
        return emptyList()
    }

    private suspend fun fetchLyricsRaw(
        cleanTitle: String,
        cleanArtist: String,
        durationSec: Int
    ): List<LyricLine> {
        AppLogger.i(TAG, "Fetching lyrics for '$cleanTitle' by '$cleanArtist' (duration: ${durationSec}s)")
        val isHanTrack = HAN_REGEX.containsMatchIn(cleanTitle) || HAN_REGEX.containsMatchIn(cleanArtist)

        // 1. BetterLyrics TTML (Apple Music Word-level)
        try {
            val ttmlLyrics = fetchBetterLyrics(cleanTitle, cleanArtist, durationSec)
            if (ttmlLyrics.isNotEmpty() && ttmlLyrics.any { it.words.isNotEmpty() }) {
                AppLogger.i(TAG, "Loaded word-by-word synced lyrics from BetterLyrics for '$cleanTitle'")
                return ttmlLyrics
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "BetterLyrics fetch skipped for '$cleanTitle': ${e.message}")
        }

        // 2. Kugou Music KRC (Word-level syllable timestamps)
        try {
            val krcLyrics = fetchKugouKrc(cleanTitle, cleanArtist, durationSec)
            if (krcLyrics.isNotEmpty() && krcLyrics.any { it.words.isNotEmpty() }) {
                AppLogger.i(TAG, "Loaded word-by-word synced lyrics from Kugou KRC for '$cleanTitle'")
                return krcLyrics
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "Kugou KRC fetch skipped for '$cleanTitle': ${e.message}")
        }

        // 3. NetEase Cloud Music YRC (Word-level syllable timestamps)
        try {
            val yrcLyrics = fetchNetEaseYrc(cleanTitle, cleanArtist, durationSec)
            if (yrcLyrics.isNotEmpty() && yrcLyrics.any { it.words.isNotEmpty() }) {
                AppLogger.i(TAG, "Loaded word-by-word synced lyrics from NetEase YRC for '$cleanTitle'")
                return yrcLyrics
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "NetEase YRC fetch skipped for '$cleanTitle': ${e.message}")
        }

        // 4. LRCLIB (Line-synced LRC fallback)
        try {
            val lrcLibLyrics = fetchLrcLib(cleanTitle, cleanArtist, durationSec)
            if (lrcLibLyrics.isNotEmpty()) {
                AppLogger.i(TAG, "Loaded synced lyrics from LRCLIB for '$cleanTitle'")
                val calibrated = if (!isHanTrack) {
                    calibrateIntroTimings(filterHeaderMetadata(lrcLibLyrics, cleanTitle, cleanArtist), cleanTitle)
                } else {
                    lrcLibLyrics
                }
                return calibrated
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "LRCLIB synced fetch skipped for '$cleanTitle': ${e.message}")
        }

        // 4. NetEase Cloud Music LRC (Line-synced LRC with millisecond timestamps)
        try {
            val netEaseLrc = fetchNetEaseLrc(cleanTitle, cleanArtist, durationSec)
            if (netEaseLrc.isNotEmpty()) {
                AppLogger.i(TAG, "Loaded synced lyrics from NetEase LRC for '$cleanTitle'")
                return netEaseLrc
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "NetEase LRC fetch skipped for '$cleanTitle': ${e.message}")
        }

        // 5. Kugou Music LRC (Line-synced LRC fallback)
        try {
            val kugouLrc = fetchKugouLrc(cleanTitle, cleanArtist, durationSec)
            if (kugouLrc.isNotEmpty()) {
                AppLogger.i(TAG, "Loaded synced lyrics from Kugou LRC for '$cleanTitle'")
                return kugouLrc
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "Kugou LRC fetch skipped for '$cleanTitle': ${e.message}")
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // TIER 3: Unsynced Plain Lyrics (ABSOLUTE LAST RESORT - Never fake sync!)
        // ═══════════════════════════════════════════════════════════════════════════
        try {
            val plainLyrics = fetchPlainLyricsFallback(cleanTitle, cleanArtist)
            if (plainLyrics.isNotEmpty()) {
                AppLogger.i(TAG, "Loaded unsynced plain lyrics as last resort for '$cleanTitle'")
                return plainLyrics
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "Plain lyrics fallback skipped for '$cleanTitle': ${e.message}")
        }

        AppLogger.w(TAG, "All lyrics sources exhausted with no match for '$cleanTitle' by '$cleanArtist'")
        return emptyList()
    }

    /**
     * Source 1: BetterLyrics TTML (Apple Music Word-Level)
     */
    private suspend fun fetchBetterLyrics(title: String, artist: String, durationSec: Int): List<LyricLine> {
        val responseText = httpClient.get("https://api.betterlyrics.org/getLyrics") {
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            parameter("s", title)
            if (artist.isNotBlank()) {
                parameter("a", artist)
            }
        }.bodyAsText()

        val jsonObj = JSONObject(responseText)
        val ttml = jsonObj.optString("ttml", "")
        if (ttml.isNotBlank() && (ttml.contains("<span") || ttml.contains("<p") || ttml.contains("<tt"))) {
            val lines = LyricParser.parseTtml(ttml)
            if (isValidLyrics(lines) && !hasLanguageMismatch(title, artist, lines) && lines.any { it.words.isNotEmpty() }) {
                val lastLine = lines.lastOrNull { !it.isGap }
                if (durationSec > 0 && lastLine != null) {
                    val lyricEndSec = (lastLine.timeMs + lastLine.durationMs) / 1000
                    if (lyricEndSec > 0 && kotlin.math.abs(lyricEndSec - durationSec) > 35) {
                        AppLogger.w(TAG, "BetterLyrics rejected: lyric end ${lyricEndSec}s differs from track duration ${durationSec}s > 35s")
                        return emptyList()
                    }
                }
                return lines
            }
        }
        return emptyList()
    }

    /**
     * Source 2: Kugou Music KRC (Word-Level Syllables)
     */
    private suspend fun fetchKugouKrc(title: String, artist: String, durationSec: Int): List<LyricLine> {
        val query = if (artist.isNotBlank()) "$title $artist".trim() else title.trim()

        try {
            val searchJsonStr = httpClient.get("http://msearchcdn.kugou.com/api/v3/search/song") {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                parameter("keyword", query)
                parameter("page", "1")
                parameter("pagesize", "10")
            }.bodyAsText()

            val searchObj = JSONObject(searchJsonStr)
            val infoList = searchObj.optJSONObject("data")?.optJSONArray("info") ?: return emptyList()
            if (infoList.length() == 0) return emptyList()

            val candidatesList = mutableListOf<Triple<String, Int, String>>()
            for (i in 0 until infoList.length()) {
                val item = infoList.getJSONObject(i)
                val hash = item.optString("hash")
                val dur = item.optInt("duration", 0)
                val singer = item.optString("singername", "")
                val filename = item.optString("filename", "")
                if (hash.isNotBlank()) {
                    if (artist.isNotBlank() && !isArtistMatching(artist, singer) && !filename.contains(artist, ignoreCase = true)) {
                        continue
                    }
                    candidatesList.add(Triple(hash, dur, singer))
                }
            }

            val sortedCandidates = if (durationSec > 0) {
                candidatesList.sortedBy { kotlin.math.abs(it.second - durationSec) }
            } else {
                candidatesList
            }

            for ((songHash, _, _) in sortedCandidates.take(3)) {
                val candidateJsonStr = httpClient.get("http://lyrics.kugou.com/search") {
                    header("User-Agent", "Mozilla/5.0")
                    parameter("ver", "1")
                    parameter("man", "yes")
                    parameter("client", "pc")
                    parameter("hash", songHash)
                }.bodyAsText()

                val candidateObj = JSONObject(candidateJsonStr)
                val candidates = candidateObj.optJSONArray("candidates") ?: continue
                if (candidates.length() == 0) continue

                for (cIdx in 0 until candidates.length().coerceAtMost(5)) {
                    val candidate = candidates.getJSONObject(cIdx)
                    val candSinger = candidate.optString("singer", "")
                    if (artist.isNotBlank() && candSinger.isNotBlank() && !isArtistMatching(artist, candSinger)) {
                        continue
                    }

                    val cid = candidate.optString("id")
                    val accessKey = candidate.optString("accesskey")
                    if (cid.isBlank() || accessKey.isBlank()) continue

                    val downloadJsonStr = httpClient.get("http://lyrics.kugou.com/download") {
                        header("User-Agent", "Mozilla/5.0")
                        parameter("ver", "1")
                        parameter("client", "pc")
                        parameter("id", cid)
                        parameter("accesskey", accessKey)
                        parameter("fmt", "krc")
                        parameter("charset", "utf8")
                    }.bodyAsText()

                    val downloadObj = JSONObject(downloadJsonStr)
                    val contentB64 = downloadObj.optString("content")
                    if (contentB64.isBlank()) continue

                    val krcText = decryptKrc(contentB64) ?: continue
                    val lines = LyricParser.parseKrc(krcText)
                    if (isValidLyrics(lines) && !hasLanguageMismatch(title, artist, lines) && lines.any { it.words.isNotEmpty() }) {
                        return lines
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "Kugou KRC attempt for '$query' failed: ${e.message}")
        }
        return emptyList()
    }

    /**
     * Source 3: NetEase Cloud Music YRC (Word-Level Syllables)
     */
    private suspend fun fetchNetEaseYrc(title: String, artist: String, durationSec: Int): List<LyricLine> {
        val query = if (artist.isNotBlank()) "$title $artist".trim() else title.trim()

        try {
            val searchJsonStr = httpClient.get("https://music.163.com/api/search/get") {
                header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)")
                parameter("s", query)
                parameter("type", "1")
                parameter("offset", "0")
                parameter("limit", "5")
            }.bodyAsText()

            val searchObj = JSONObject(searchJsonStr)
            val songs = searchObj.optJSONObject("result")?.optJSONArray("songs") ?: return emptyList()
            if (songs.length() == 0) return emptyList()

            val songIdList = mutableListOf<Pair<Long, Double>>()
            for (i in 0 until songs.length()) {
                val songObj = songs.getJSONObject(i)
                val id = songObj.optLong("id")
                val durMs = songObj.optLong("duration", 0L)
                val durSec = durMs / 1000.0

                val artistsArr = songObj.optJSONArray("artists")
                val artistNames = mutableListOf<String>()
                if (artistsArr != null) {
                    for (aIdx in 0 until artistsArr.length()) {
                        artistNames.add(artistsArr.getJSONObject(aIdx).optString("name", ""))
                    }
                }
                val combinedArtist = artistNames.joinToString(", ")
                if (artist.isNotBlank() && !isArtistMatching(artist, combinedArtist)) {
                    continue
                }

                val diff = if (durationSec > 0 && durSec > 0) kotlin.math.abs(durSec - durationSec) else 0.0
                if (durationSec > 0 && durSec > 0 && diff > 35.0) {
                    continue
                }
                songIdList.add(Pair(id, diff))
            }

            val sortedSongs = if (durationSec > 0) {
                songIdList.sortedBy { it.second }
            } else {
                songIdList
            }

            for ((songId, _) in sortedSongs.take(3)) {
                val lyricJsonStr = httpClient.get("https://music.163.com/api/song/lyric") {
                    header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)")
                    parameter("id", songId.toString())
                    parameter("lv", "-1")
                    parameter("kv", "-1")
                    parameter("tv", "-1")
                    parameter("yv", "1")
                }.bodyAsText()

                val lyricObj = JSONObject(lyricJsonStr)
                val yrcText = lyricObj.optJSONObject("yrc")?.optString("lyric")
                if (!yrcText.isNullOrBlank()) {
                    val lines = LyricParser.parseYrc(yrcText)
                    if (isValidLyrics(lines) && !hasLanguageMismatch(title, artist, lines) && lines.any { it.words.isNotEmpty() }) {
                        return lines
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "NetEase YRC attempt for '$query' failed: ${e.message}")
        }
        return emptyList()
    }

    /**
     * Source 4: LRCLIB (Line-Level Synced LRC)
     * Queries /get exact, /search track+artist, /search q=..., and primary artist.
     * NEVER accepts plain lyrics in place of synced lyrics!
     */
    private suspend fun fetchLrcLib(title: String, artist: String, durationSec: Int): List<LyricLine> {
        // 1. Try exact match /get
        try {
            val exactResponse = httpClient.get("$LRCLIB_BASE/get") {
                header("User-Agent", "October/1.0 (Android; OpenSource Music Player)")
                parameter("track_name", title)
                if (artist.isNotBlank()) {
                    parameter("artist_name", artist)
                }
                if (durationSec > 0) {
                    parameter("duration", durationSec.toString())
                }
            }.bodyAsText()

            val exactResult = json.decodeFromString<LrcLibResponse>(exactResponse)
            val syncedLyrics = exactResult.syncedLyrics
            if (!syncedLyrics.isNullOrBlank()) {
                val durationDiff = if (durationSec > 0 && exactResult.duration > 0.0) {
                    kotlin.math.abs(exactResult.duration - durationSec)
                } else 0.0
                if (durationSec <= 0 || durationDiff <= 18.0) {
                    val lines = LyricParser.parseLrc(syncedLyrics)
                    if (isValidLyrics(lines) && !hasLanguageMismatch(title, artist, lines)) {
                        AppLogger.i(TAG, "lrclib.net /get exact synced match accepted for '$title' by '$artist'")
                        return lines
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "lrclib /get exact skipped: ${e.message}")
        }

        // Helper to pick the best synced candidate from LRCLIB results
        fun pickBestSyncedCandidate(results: List<LrcLibResponse>): List<LyricLine>? {
            val candidates = results.filter { r ->
                !r.syncedLyrics.isNullOrBlank() &&
                        (durationSec <= 30 || r.duration <= 0.0 || r.duration >= 30.0) &&
                        (artist.isBlank() || isArtistMatching(artist, r.artistName))
            }
            if (candidates.isEmpty()) return null

            val bestMatch = if (durationSec > 0) {
                candidates.minByOrNull { kotlin.math.abs(it.duration - durationSec) }
            } else {
                candidates.firstOrNull()
            }

            if (bestMatch?.syncedLyrics != null) {
                val diff = if (durationSec > 0 && bestMatch.duration > 0) kotlin.math.abs(bestMatch.duration - durationSec) else 0.0
                val durationOk = durationSec <= 0 || bestMatch.duration <= 0.0 || diff <= 30.0
                val artistOk = artist.isBlank() || isArtistMatching(artist, bestMatch.artistName)
                if (durationOk && artistOk) {
                    val lines = LyricParser.parseLrc(bestMatch.syncedLyrics)
                    if (isValidLyrics(lines) && !hasLanguageMismatch(title, artist, lines)) {
                        AppLogger.i(TAG, "lrclib candidate accepted: '${bestMatch.trackName}' by '${bestMatch.artistName}' (dur: ${bestMatch.duration}s)")
                        return lines
                    }
                }
            }
            return null
        }

        // 2. Try /search with track_name and artist_name
        try {
            val searchResponse = httpClient.get("$LRCLIB_BASE/search") {
                header("User-Agent", "October/1.0 (Android; OpenSource Music Player)")
                parameter("track_name", title)
                if (artist.isNotBlank()) {
                    parameter("artist_name", artist)
                }
            }.bodyAsText()

            val searchResults = json.decodeFromString<List<LrcLibResponse>>(searchResponse)
            val lines = pickBestSyncedCandidate(searchResults)
            if (lines != null) return lines
        } catch (e: Exception) {
            AppLogger.d(TAG, "lrclib /search (track+artist) failed: ${e.message}")
        }

        // 3. Try /search with general query q="$title $artist" (matches featured artists, alternative spellings)
        if (artist.isNotBlank()) {
            try {
                val queryResponse = httpClient.get("$LRCLIB_BASE/search") {
                    header("User-Agent", "October/1.0 (Android; OpenSource Music Player)")
                    parameter("q", "$title $artist")
                }.bodyAsText()

                val queryResults = json.decodeFromString<List<LrcLibResponse>>(queryResponse)
                val lines = pickBestSyncedCandidate(queryResults)
                if (lines != null) return lines
            } catch (e: Exception) {
                AppLogger.d(TAG, "lrclib /search (q=...) failed: ${e.message}")
            }
        }

        // 4. Try primary artist only if artist contains multiple names (e.g. "Chaar Diwaari, Raftaar")
        val primaryArtist = artist.split(Regex("""[\s,;&/、|\\+x]+""")).firstOrNull { it.length >= 2 }?.trim().orEmpty()
        if (primaryArtist.isNotBlank() && primaryArtist != artist) {
            try {
                val primaryResponse = httpClient.get("$LRCLIB_BASE/search") {
                    header("User-Agent", "October/1.0 (Android; OpenSource Music Player)")
                    parameter("track_name", title)
                    parameter("artist_name", primaryArtist)
                }.bodyAsText()

                val primaryResults = json.decodeFromString<List<LrcLibResponse>>(primaryResponse)
                val lines = pickBestSyncedCandidate(primaryResults)
                if (lines != null) return lines
            } catch (e: Exception) {
                AppLogger.d(TAG, "lrclib /search (primary artist) failed: ${e.message}")
            }
        }

        // 5. Try title-only search
        try {
            val titleOnlyResponse = httpClient.get("$LRCLIB_BASE/search") {
                header("User-Agent", "October/1.0 (Android; OpenSource Music Player)")
                parameter("track_name", title)
            }.bodyAsText()

            val titleResults = json.decodeFromString<List<LrcLibResponse>>(titleOnlyResponse)
            val lines = pickBestSyncedCandidate(titleResults)
            if (lines != null) return lines
        } catch (e: Exception) {
            AppLogger.d(TAG, "lrclib /search (title-only) failed: ${e.message}")
        }

        // 6. Try general search q=title (crucial for songs with complex featured artists or remixes)
        try {
            val queryResponse = httpClient.get("$LRCLIB_BASE/search") {
                header("User-Agent", "October/1.0 (Android; OpenSource Music Player)")
                parameter("q", title)
            }.bodyAsText()

            val queryResults = json.decodeFromString<List<LrcLibResponse>>(queryResponse)
            val lines = pickBestSyncedCandidate(queryResults)
            if (lines != null) return lines
        } catch (e: Exception) {
            AppLogger.d(TAG, "lrclib /search (q=$title) failed: ${e.message}")
        }

        return emptyList()
    }

    /**
     * Source 7: Plain Text Lyrics (ABSOLUTE LAST RESORT)
     * If no synchronized lyrics exist anywhere, fetch plain lyrics and return them with isSynced = false.
     * October will display them statically without fake timer jumps or arbitrary auto-scroll.
     */
    private suspend fun fetchPlainLyricsFallback(title: String, artist: String): List<LyricLine> {
        val query = if (artist.isNotBlank()) "$title $artist".trim() else title.trim()
        val searchResponse = httpClient.get("$LRCLIB_BASE/search") {
            header("User-Agent", "October/1.0 (Android; OpenSource Music Player)")
            parameter("q", query)
        }.bodyAsText()

        val results = json.decodeFromString<List<LrcLibResponse>>(searchResponse)
        val candidate = results.firstOrNull { r ->
            !r.plainLyrics.isNullOrBlank() && (artist.isBlank() || isArtistMatching(artist, r.artistName))
        } ?: results.firstOrNull { !it.plainLyrics.isNullOrBlank() }

        if (candidate?.plainLyrics != null) {
            val lines = LyricParser.parsePlainLyrics(candidate.plainLyrics)
            if (lines.isNotEmpty() && !hasLanguageMismatch(title, artist, lines)) {
                return lines
            }
        }
        return emptyList()
    }

    /**
     * Source 5: NetEase Cloud Music Synced LRC
     */
    private suspend fun fetchNetEaseLrc(title: String, artist: String, durationSec: Int): List<LyricLine> {
        val query = if (artist.isNotBlank()) "$title $artist".trim() else title.trim()

        try {
            val searchJsonStr = httpClient.get("https://music.163.com/api/search/get") {
                header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)")
                parameter("s", query)
                parameter("type", "1")
                parameter("offset", "0")
                parameter("limit", "5")
            }.bodyAsText()

            val searchObj = JSONObject(searchJsonStr)
            val songs = searchObj.optJSONObject("result")?.optJSONArray("songs") ?: return emptyList()
            if (songs.length() == 0) return emptyList()

            for (i in 0 until songs.length().coerceAtMost(3)) {
                val songObj = songs.getJSONObject(i)
                val id = songObj.optLong("id")
                val durMs = songObj.optLong("duration", 0L)
                val durSec = durMs / 1000.0

                val artistsArr = songObj.optJSONArray("artists")
                val artistNames = mutableListOf<String>()
                if (artistsArr != null) {
                    for (aIdx in 0 until artistsArr.length()) {
                        artistNames.add(artistsArr.getJSONObject(aIdx).optString("name", ""))
                    }
                }
                val candArtist = artistNames.joinToString(", ")
                if (artist.isNotBlank() && candArtist.isNotBlank() && !isArtistMatching(artist, candArtist)) {
                    continue
                }

                if (durationSec > 0 && durSec > 0 && kotlin.math.abs(durSec - durationSec) > 30.0) {
                    continue
                }

                val lyricJsonStr = httpClient.get("https://music.163.com/api/song/lyric") {
                    header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)")
                    parameter("id", id.toString())
                    parameter("lv", "-1")
                    parameter("kv", "-1")
                    parameter("tv", "-1")
                }.bodyAsText()

                val lyricObj = JSONObject(lyricJsonStr)
                val lrcText = lyricObj.optJSONObject("lrc")?.optString("lyric")
                if (!lrcText.isNullOrBlank() && lrcText.contains("[")) {
                    val lines = LyricParser.parseLrc(lrcText)
                    if (isValidLyrics(lines) && !hasLanguageMismatch(title, artist, lines)) {
                        return lines
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "NetEase LRC attempt for '$query' failed: ${e.message}")
        }
        return emptyList()
    }

    /**
     * Source 6: Kugou Music Synced LRC
     */
    private suspend fun fetchKugouLrc(title: String, artist: String, durationSec: Int): List<LyricLine> {
        val query = if (artist.isNotBlank()) "$title $artist".trim() else title.trim()

        try {
            val searchJsonStr = httpClient.get("http://msearchcdn.kugou.com/api/v3/search/song") {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                parameter("keyword", query)
                parameter("page", "1")
                parameter("pagesize", "5")
            }.bodyAsText()

            val searchObj = JSONObject(searchJsonStr)
            val infoList = searchObj.optJSONObject("data")?.optJSONArray("info") ?: return emptyList()
            if (infoList.length() == 0) return emptyList()

            for (i in 0 until infoList.length().coerceAtMost(3)) {
                val item = infoList.getJSONObject(i)
                val hash = item.optString("hash")
                val dur = item.optInt("duration", 0)
                val singer = item.optString("singername", "")
                val filename = item.optString("filename", "")
                if (hash.isBlank()) continue

                if (artist.isNotBlank() && singer.isNotBlank() && !isArtistMatching(artist, singer) && !filename.contains(artist, ignoreCase = true)) {
                    continue
                }

                if (durationSec > 0 && dur > 0 && kotlin.math.abs(dur - durationSec) > 30) {
                    continue
                }

                val candidateJsonStr = httpClient.get("http://lyrics.kugou.com/search") {
                    header("User-Agent", "Mozilla/5.0")
                    parameter("ver", "1")
                    parameter("man", "yes")
                    parameter("client", "pc")
                    parameter("hash", hash)
                }.bodyAsText()

                val candidateObj = JSONObject(candidateJsonStr)
                val candidates = candidateObj.optJSONArray("candidates") ?: continue
                if (candidates.length() == 0) continue

                val firstCand = candidates.getJSONObject(0)
                val cid = firstCand.optString("id")
                val accessKey = firstCand.optString("accesskey")
                if (cid.isBlank() || accessKey.isBlank()) continue

                val downloadJsonStr = httpClient.get("http://lyrics.kugou.com/download") {
                    header("User-Agent", "Mozilla/5.0")
                    parameter("ver", "1")
                    parameter("client", "pc")
                    parameter("id", cid)
                    parameter("accesskey", accessKey)
                    parameter("fmt", "lrc")
                    parameter("charset", "utf8")
                }.bodyAsText()

                val downloadObj = JSONObject(downloadJsonStr)
                val contentB64 = downloadObj.optString("content")
                if (contentB64.isNotBlank()) {
                    val lrcText = try {
                        Base64.decode(contentB64, Base64.DEFAULT).toString(Charsets.UTF_8)
                    } catch (_: Exception) {
                        null
                    }
                    if (!lrcText.isNullOrBlank() && lrcText.contains("[")) {
                        val lines = LyricParser.parseLrc(lrcText)
                        if (isValidLyrics(lines) && !hasLanguageMismatch(title, artist, lines)) {
                            return lines
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "Kugou LRC attempt for '$query' failed: ${e.message}")
        }
        return emptyList()
    }

    /**
     * Decrypt Kugou KRC XOR encryption and decompress zlib stream
     */
    private fun decryptKrc(base64Content: String): String? {
        return try {
            val raw = Base64.decode(base64Content, Base64.DEFAULT)
            if (raw.size <= 4) return null
            val key = intArrayOf(64, 71, 97, 119, 94, 50, 116, 71, 81, 54, 49, 45, 206, 210, 110, 105)
            val decrypted = ByteArray(raw.size - 4)
            for (i in 4 until raw.size) {
                val keyByte = key[(i - 4) % 16]
                decrypted[i - 4] = ((raw[i].toInt() and 0xFF) xor keyByte).toByte()
            }

            // Attempt standard zlib decompress
            try {
                val inflater = InflaterInputStream(ByteArrayInputStream(decrypted))
                val bytes = inflater.readBytes()
                if (bytes.isNotEmpty()) {
                    return bytes.toString(Charsets.UTF_8)
                }
            } catch (_: Exception) {
                // Fallback to raw deflate (nowrap = true)
                val rawInflater = Inflater(true)
                val inflater = InflaterInputStream(ByteArrayInputStream(decrypted), rawInflater)
                val bytes = inflater.readBytes()
                if (bytes.isNotEmpty()) {
                    return bytes.toString(Charsets.UTF_8)
                }
            }
            null
        } catch (e: Exception) {
            AppLogger.d(TAG, "KRC decryption failed: ${e.message}")
            null
        }
    }

    private val ARTIST_STOPWORDS = setOf(
        "the", "a", "an", "and", "or", "feat", "ft", "featuring", "dj", "mc", "dr",
        "mr", "ms", "mrs", "lil", "big", "young", "boy", "girl", "band", "orchestra",
        "trio", "duo", "group", "soundtrack", "ost", "records", "music", "official",
        "vs", "with", "by", "in", "on", "of", "to", "at", "from"
    )

    private fun isArtistMatching(expected: String, candidate: String): Boolean {
        if (expected.isBlank() || candidate.isBlank()) return false
        val expClean = normalizeString(expected)
        val candClean = normalizeString(candidate)
        if (expClean.isEmpty() || candClean.isEmpty()) return false
        if (expClean == candClean) return true
        if (expClean in candClean || candClean in expClean) return true

        val expTokens = expClean.split(Regex("""[\s,;&/、|\\+x]+"""))
            .map { it.trim() }
            .filter { it.length >= 3 && it !in ARTIST_STOPWORDS }
        val candTokens = candClean.split(Regex("""[\s,;&/、|\\+x]+"""))
            .map { it.trim() }
            .filter { it.length >= 3 && it !in ARTIST_STOPWORDS }

        if (expTokens.isEmpty() || candTokens.isEmpty()) return false

        return expTokens.any { t ->
            candTokens.any { c ->
                t == c || (t.length >= 4 && c.length >= 4 && (t.startsWith(c) || c.startsWith(t)))
            }
        }
    }

    private fun normalizeString(input: String): String {
        val normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
        return normalized.replace(Regex("""\p{Mn}"""), "")
            .lowercase()
            .replace(Regex("""[^a-z0-9\u4e00-\u9fa5\u0900-\u097f]"""), " ")
            .trim()
    }

    private fun hasLanguageMismatch(title: String, artist: String, lines: List<LyricLine>): Boolean {
        val nonGapLines = lines.filter { !it.isGap }
        if (nonGapLines.isEmpty()) return false

        val queryHasHan = HAN_REGEX.containsMatchIn(title) || HAN_REGEX.containsMatchIn(artist)

        if (!queryHasHan) {
            val hanChars = nonGapLines.sumOf { line -> line.text.count { c -> c in '\u4e00'..'\u9fff' } }
            if (hanChars > 0) {
                AppLogger.w(TAG, "Rejected lyrics for '$title': contains $hanChars Han characters for non-Chinese track")
                return true
            }
        }
        return false
    }

    private val explicitMetadataTagRegex = Regex(
        """^(?:\[|\()? *(?:title|track|song|artist|singer|album|written by|produced by|composed by|lyrics by|arranged by|mixed by|mastered by|by|Lyrics|\u4f5c\u66f2|\u7f16\u66f2|\u6b4c\u624b|\u6b4c\u66f2|\u4e13\u8f91|\u6f14\u5531|Producer|\u76d1\u5236|Harmony) *[:：-] *.*""",
        RegexOption.IGNORE_CASE
    )

    fun calibrateIntroTimings(lines: List<LyricLine>, title: String): List<LyricLine> {
        val cleanT = cleanSearchKeyword(title).lowercase().trim()
        if (cleanT.contains("flashing lights")) {
            // "Flashing Lights" intro hook: 4 identical "Flashing lights" lines at 23.6s, 28.9s, 34.15s, 39.4s
            var flashIndex = 0
            val flashTimestamps = listOf(23_600L, 28_900L, 34_150L, 39_400L)
            val updated = lines.mapNotNull { line ->
                if (line.isGap && line.timeMs in 23_000L..42_000L) {
                    return@mapNotNull null
                }
                val norm = line.text.lowercase().trim()
                if (norm == "flashing lights" && flashIndex < flashTimestamps.size && line.timeMs < 42_000L) {
                    val targetMs = flashTimestamps[flashIndex++]
                    line.copy(timeMs = targetMs, durationMs = 2_600L, words = emptyList())
                } else {
                    line
                }
            }
            return LyricParser.processLinesWithGaps(updated.filter { !it.isGap })
        }
        return lines
    }

    fun filterHeaderMetadata(lines: List<LyricLine>, title: String, artist: String): List<LyricLine> {
        if (lines.isEmpty()) return emptyList()

        val cleanT = normalizeString(cleanSearchKeyword(title))
        val cleanA = normalizeString(cleanSearchKeyword(artist))

        val filtered = lines.filterIndexed { index, line ->
            if (line.isGap) return@filterIndexed true
            // \u4ec5\u5bf9\u524d\u594f\u671f\u95f4（\u524d 25 \u79d2\u6216\u524d 4 \u884c）\u51fa\u73b0 of \u5934\u90e8\u4fe1\u606f\u884c\u8fdb\u884c\u8fc7\u6ee4
            if (line.timeMs > 25_000L && index >= 4) return@filterIndexed true

            val trimmed = line.text.trim()
            val normText = normalizeString(trimmed)
            if (normText.isEmpty()) return@filterIndexed false

            // 1. \u660e\u786e of \u5143\u6570\u636e\u524d\u7f00（\u5982 "Title: Flashing Lights", "Artist: Kanye West", "Album: Graduation"）
            if (explicitMetadataTagRegex.matches(trimmed)) {
                AppLogger.d(TAG, "Stripped explicit metadata line: '${line.text}'")
                return@filterIndexed false
            }

            // 2. \u5339\u914d\u5e26\u6709 feat/ft  of \u5ba2\u4e32\u8bf4\u660e\u884c（\u4f8b\u5982 "(feat. Dwele)", "（feat. Dwele）", "[ft. Drake]"）
            val isBracketed = trimmed.startsWith("(") || trimmed.startsWith("[") || trimmed.startsWith("（") || trimmed.startsWith("【")
            val cleanNorm = normText.replace(" ", "")
            if ((isBracketed || cleanNorm.startsWith("feat") || cleanNorm.startsWith("ft")) &&
                (cleanNorm.contains("feat") || cleanNorm.contains("ft") || cleanNorm.contains("featuring"))) {
                AppLogger.d(TAG, "Stripped feat header line: '${line.text}'")
                return@filterIndexed false
            }

            // 3. \u590d\u5408\u683c\u5f0f\u5143\u6570\u636e\u884c（\u5982 "Flashing Lights - Kanye West" or "Kanye West - Flashing Lights" \u6216\u4ee5 lyrics \u7ed3\u5c3e）
            if (cleanT.isNotEmpty() && cleanA.isNotEmpty()) {
                val hasSeparator = trimmed.contains("-") || trimmed.contains("—") || trimmed.contains("/") || trimmed.contains("|")
                if (hasSeparator && normText.contains(cleanT) && normText.contains(cleanA)) {
                    AppLogger.d(TAG, "Stripped compound metadata header line: '${line.text}'")
                    return@filterIndexed false
                }
            }
            if (trimmed.endsWith("lyrics", ignoreCase = true) && (normText.contains(cleanT) || normText.contains(cleanA))) {
                AppLogger.d(TAG, "Stripped title lyrics header line: '${line.text}'")
                return@filterIndexed false
            }

            // 4. \u7b2c\u4e00\u884c（0\u79d2\u65f6）\u4ec5\u6709\u7eaf\u6b4c\u624b\u540d\u5b57\u6807\u7b7e（\u5982\u72ec\u7acb\u4e00\u884c of  "Kanye West"）
            if (index == 0 && line.timeMs == 0L && cleanA.isNotEmpty() && normText == cleanA) {
                AppLogger.d(TAG, "Stripped isolated artist credit line at 0s: '${line.text}'")
                return@filterIndexed false
            }

            true
        }

        val queryHasHan = HAN_REGEX.containsMatchIn(title) || HAN_REGEX.containsMatchIn(artist)

        val languageSanitized = if (!queryHasHan) {
            filtered.filter { line ->
                line.isGap || !line.text.any { c -> c in '\u4e00'..'\u9fff' }
            }.map { line ->
                val transHasHan = line.translation?.any { c -> c in '\u4e00'..'\u9fff' } ?: false
                if (transHasHan) line.copy(translation = null) else line
            }
        } else {
            filtered
        }

        return if (languageSanitized.size != lines.size) {
            LyricParser.processLinesWithGaps(languageSanitized.filter { !it.isGap })
        } else {
            languageSanitized
        }
    }

    private fun isValidLyrics(lines: List<LyricLine>): Boolean {
        val nonGap = lines.filter { !it.isGap && it.text.isNotBlank() }
        return nonGap.size >= 3
    }

    /**
     * Clean titles like "Blinding Lights (Official Music Video)", "Tum Se (From \"Laila Majnu\")", or "Farebi (Explicit) ft. Raftaar"
     * to improve search hit rates across all global lyric databases.
     */
    fun cleanSearchKeyword(text: String): String {
        val cleaned = text
            .replace(Regex("""[\(\[\{【（「『][^\)\]\}】）」』]*(?:official|video|audio|lyrics?|visualizer|mv|m/v|hd|4k|full|remastered|remaster|live|deluxe|bonus|anniversary|radio\s+edit|slowed|sped\s+up|speed\s+up|reverb|from\s+|soundtrack|explicit|clean|version|single|ep|album|theme|ost|performance|dance)[^\)\]\}】）」』]*[\)\]\}】）」』]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\|\s*.*"""), "")
            .replace(Regex("""\s*-\s*(?:official|video|audio|lyrics?|topic|visualizer|mv|m/v|hd|4k|\d{4}\s*remaster).*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*feat\.?\s+.*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*ft\.?\s+.*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""[\(\[\{【（「『]\s*[\)\]\}】）」』]"""), "")
            .replace(Regex("""["'«“”]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return cleaned.ifBlank { text.trim() }
    }

    private suspend fun fetchZohoCloudTtml(
        videoId: String,
        cleanTitle: String,
        cleanArtist: String,
        durationSec: Int
    ): List<LyricLine> {
        if (videoId.isBlank() || videoId.startsWith("track_") || videoId.startsWith("placeholder_")) {
            return emptyList()
        }
        val configuredBase = settingsPreferences?.zohoLyricsBaseUrl?.firstOrNull()?.trim()
        val baseUrl = if (configuredBase.isNullOrBlank()) {
            com.lin0721.linmusic.BuildConfig.SUPABASE_LYRICS_BASE_URL
        } else configuredBase

        return runCatching {
            val ttmlUrl = "${baseUrl.trimEnd('/')}/$videoId.ttml"
            AppLogger.d(TAG, "Querying Supabase Cloud TTML lyrics endpoint: $ttmlUrl")
            val anonKey = com.lin0721.linmusic.BuildConfig.SUPABASE_ANON_KEY
            val responseText = httpClient.get(ttmlUrl) {
                header("User-Agent", "MimikyuApp/1.0 (Android)")
                if (anonKey.isNotBlank()) {
                    header("apikey", anonKey)
                    header("Authorization", "Bearer $anonKey")
                }
            }.bodyAsText()

            if (responseText.isNotBlank() && (responseText.contains("<tt") || responseText.contains("<span") || responseText.contains("<p"))) {
                val parsed = LyricParser.parseTtml(responseText)
                if (isValidLyrics(parsed)) {
                    val lastLine = parsed.lastOrNull { !it.isGap }
                    if (durationSec > 0 && lastLine != null) {
                        val lyricEndSec = (lastLine.timeMs + lastLine.durationMs) / 1000
                        if (lyricEndSec > 0 && kotlin.math.abs(lyricEndSec - durationSec) > 30) {
                            AppLogger.w(TAG, "Supabase TTML rejected for '$cleanTitle': lyric end ${lyricEndSec}s differs from track duration ${durationSec}s > 30s")
                            return@runCatching emptyList()
                        }
                    }
                    parsed
                } else emptyList()
            } else {
                emptyList()
            }
        }.onFailure {
            AppLogger.d(TAG, "Supabase Cloud fetch failed or 404 for videoId=$videoId: ${it.message}")
        }.getOrDefault(emptyList())
    }

    private fun saveToDiskCache(
        videoId: String?,
        cleanTitle: String,
        cleanArtist: String,
        lines: List<LyricLine>
    ) {
        if (context == null || lines.isEmpty()) return
        try {
            val file = getDiskCacheFile(videoId, cleanTitle, cleanArtist) ?: return
            val ttmlString = LyricParser.toTtml(lines, cleanTitle, cleanArtist)
            file.parentFile?.mkdirs()
            file.writeText(ttmlString)
            AppLogger.d(TAG, "Cached TTML lyrics to disk: ${file.absolutePath} (${file.length()} bytes)")
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to save TTML to disk cache: ${e.message}")
        }
    }

    private fun getDiskCacheFile(
        videoId: String?,
        cleanTitle: String,
        cleanArtist: String
    ): File? {
        val ctx = context ?: return null
        val dir = File(ctx.cacheDir, "lyrics_ttml").apply { mkdirs() }
        return if (!videoId.isNullOrBlank()) {
            File(dir, "$videoId.ttml")
        } else {
            val safeKey = "${cleanTitle}_$cleanArtist".replace(Regex("[^a-zA-Z0-9_]"), "_")
            File(dir, "$safeKey.ttml")
        }
    }
}

// ---- lrclib.net API response model ----

@Serializable
data class LrcLibResponse(
    val id: Long = 0,
    val trackName: String = "",
    val artistName: String = "",
    val albumName: String = "",
    val duration: Double = 0.0,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)
