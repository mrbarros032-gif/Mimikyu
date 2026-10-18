package com.lin0721.linmusic.core.player.resolver

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.network.AppError
import com.lin0721.linmusic.core.player.innertube.AudioQuality
import com.lin0721.linmusic.core.player.innertube.InnerTubeXPlayer
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import com.lin0721.linmusic.core.preferences.SettingsPreferences
import com.lin0721.linmusic.core.qobuz.QobuzService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "Playback"
private const val DEFAULT_STREAM_TTL_MS = 4 * 60 * 60 * 1000L // 4 hours conservative fallback

class PlaybackResolverImpl(
    private val context: Context,
    private val qobuzService: QobuzService? = null,
    private val settingsPreferences: SettingsPreferences? = null
) : PlaybackResolver {

    private val cache = ConcurrentHashMap<String, ResolvedPlaybackData>()
    private val resolutionLocks = ConcurrentHashMap<String, Mutex>()

    init {
        InnerTubeXPlayer.initialize(context)
        YouTubeIdRegistry.initialize(context)
    }

    override fun peek(videoId: String): ResolvedPlaybackData? {
        val cached = cache[videoId] ?: return null
        return if (!cached.isExpired()) cached else null
    }

    override fun invalidate(videoId: String) {
        AppLogger.i(TAG, "[Playback] videoId=$videoId cache invalidated")
        cache.remove(videoId)
        InnerTubeXPlayer.clearStreamClientFailures()
    }

    override suspend fun refresh(
        videoId: String,
        songId: Long?,
        title: String?,
        artist: String?,
        durationSec: Int
    ): Result<ResolvedPlaybackData> {
        AppLogger.i(TAG, "[Playback] videoId=$videoId refreshing stream")
        invalidate(videoId)
        return resolve(videoId, songId, title, artist, durationSec, forceRefresh = true)
    }

    override suspend fun resolveWithRetry(
        videoId: String,
        songId: Long?,
        title: String?,
        artist: String?,
        durationSec: Int,
        maxRetries: Int
    ): Result<ResolvedPlaybackData> {
        var lastError: Throwable? = null
        val boundedRetries = maxRetries.coerceIn(1, 3)

        for (attempt in 0..boundedRetries) {
            if (attempt > 0) {
                AppLogger.i(TAG, "[Playback] videoId=$videoId retry attempt=$attempt")
                delay(attempt * 1000L)
            }
            val result = resolve(
                videoId = videoId,
                songId = songId,
                title = title,
                artist = artist,
                durationSec = durationSec,
                forceRefresh = attempt > 0
            )
            if (result.isSuccess) {
                return result
            }
            lastError = result.exceptionOrNull()
            AppLogger.w(TAG, "[Playback] videoId=$videoId attempt $attempt failed: ${lastError?.message}")
        }

        return Result.failure(lastError ?: AppError.BizError(-1, "Stream resolution exhausted $boundedRetries retries"))
    }

    override suspend fun resolve(
        videoId: String,
        songId: Long?,
        title: String?,
        artist: String?,
        durationSec: Int,
        forceRefresh: Boolean
    ): Result<ResolvedPlaybackData> {
        if (videoId.isBlank()) {
            return Result.failure(IllegalArgumentException("videoId cannot be blank"))
        }

        // 1. Check in-memory unexpired cache unless forceRefresh
        if (!forceRefresh) {
            val existing = cache[videoId]
            if (existing != null && !existing.isExpired()) {
                AppLogger.d(TAG, "[Playback] videoId=$videoId cache hit (expires in ${(existing.expiresAtMs - System.currentTimeMillis()) / 1000}s)")
                return Result.success(existing)
            }
        }

        val mutex = resolutionLocks.computeIfAbsent(videoId) { Mutex() }
        return mutex.withLock {
            // Re-check after obtaining lock
            if (!forceRefresh) {
                val existing = cache[videoId]
                if (existing != null && !existing.isExpired()) {
                    return@withLock Result.success(existing)
                }
            }

            AppLogger.i(TAG, "[Playback] videoId=$videoId resolving direct audio stream (forceRefresh=$forceRefresh)")

            // Direct InnerTube audio stream extraction (audio-only: Opus / AAC)
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val isSlow = com.lin0721.linmusic.core.player.NetworkSpeedMonitor.isVerySlowInternet(context)
            val audioQuality = if (isSlow) AudioQuality.LOW else AudioQuality.HIGH

            val ytResult = InnerTubeXPlayer.playerResponseForPlayback(
                videoId = videoId,
                audioQuality = audioQuality,
                connectivityManager = cm
            )

            ytResult.fold(
                onSuccess = { playbackData ->
                    val now = System.currentTimeMillis()
                    val expiresAtMs = parseExpiresAtMs(playbackData.streamUrl, playbackData.streamExpiresInSeconds, now)

                    val altQuality = if (audioQuality == AudioQuality.HIGH) AudioQuality.LOW else AudioQuality.HIGH
                    val altResult = runCatching {
                        InnerTubeXPlayer.playerResponseForPlayback(
                            videoId = videoId,
                            audioQuality = altQuality,
                            connectivityManager = cm
                        ).getOrNull()
                    }.getOrNull()

                    val highUrl = if (audioQuality == AudioQuality.HIGH) playbackData.streamUrl else altResult?.streamUrl ?: playbackData.streamUrl
                    val lowUrl = if (audioQuality == AudioQuality.LOW) playbackData.streamUrl else altResult?.streamUrl ?: playbackData.streamUrl

                    val rawCover = YouTubeIdRegistry.getCoverUrl(songId ?: -1L)
                    val coverHigh = if (rawCover.contains("=w") || rawCover.contains("=s")) {
                        rawCover.replace(Regex("=w\\d+-h\\d+.*$"), "=w800-h800-l90-rj")
                    } else rawCover
                    val coverFast = if (rawCover.contains("=w") || rawCover.contains("=s")) {
                        rawCover.replace(Regex("=w\\d+-h\\d+.*$"), "=w300-h300-l80-rj")
                    } else rawCover

                    val resolved = ResolvedPlaybackData(
                        videoId = videoId,
                        streamUrl = playbackData.streamUrl,
                        expiresAtMs = expiresAtMs,
                        streamClient = playbackData.streamClient,
                        streamHeaders = playbackData.streamHeaders,
                        audioQuality = audioQuality,
                        itag = playbackData.format.itag,
                        mimeType = playbackData.format.mimeType,
                        contentLength = playbackData.format.contentLength,
                        isLossless = false,
                        highQualityUrl = highUrl,
                        lowQualityUrl = lowUrl,
                        coverUrlHigh = coverHigh,
                        coverUrlFast = coverFast
                    )

                    cache[videoId] = resolved
                    AppLogger.i(TAG, "[Playback] videoId=$videoId stream resolved expiresAt=${Date(expiresAtMs)} client=${resolved.streamClient} itag=${resolved.itag} (highUrl=${highUrl.take(40)}... lowUrl=${lowUrl.take(40)}...)")
                    Result.success(resolved)
                },
                onFailure = { error ->
                    AppLogger.e(TAG, "[Playback] videoId=$videoId stream resolution failed error=${error.message}", error)
                    Result.failure(error)
                }
            )
        }
    }

    companion object {
        const val DEFAULT_STREAM_TTL_MS = 4 * 60 * 60 * 1000L // 4 hours conservative fallback

        /**
         * Extracts exact expiration timestamp from the YouTube URL `&expire=` query parameter,
         * or uses the extractor's [streamExpiresInSeconds], falling back conservatively to 4 hours.
         */
        fun parseExpiresAtMs(
            url: String,
            streamExpiresInSeconds: Int? = null,
            nowMs: Long = System.currentTimeMillis()
        ): Long {
            try {
                val match = Regex("""[?&]expire=([0-9]+)""").find(url)
                if (match != null) {
                    val epochSec = match.groupValues[1].toLongOrNull()
                    if (epochSec != null && epochSec > 0) {
                        return epochSec * 1000L
                    }
                }
            } catch (_: Exception) { }

            if (streamExpiresInSeconds != null && streamExpiresInSeconds > 0) {
                return nowMs + (streamExpiresInSeconds * 1000L)
            }

            return nowMs + DEFAULT_STREAM_TTL_MS
        }
    }
}
