package com.lin0721.linmusic.core.player.resolver

import com.lin0721.linmusic.core.player.innertube.AudioQuality
import java.util.Date

/**
 * Resolved runtime stream playback data.
 * The stream URL is strictly temporary and disposable with an explicit expiration timestamp.
 */
data class ResolvedPlaybackData(
    val videoId: String,
    val streamUrl: String,
    val expiresAtMs: Long,
    val streamClient: String = "unknown",
    val streamHeaders: Map<String, String> = emptyMap(),
    val audioQuality: AudioQuality = AudioQuality.AUTO,
    val itag: Int? = null,
    val mimeType: String? = null,
    val contentLength: Long? = null,
    val isLossless: Boolean = false,
    val resolvedAtMs: Long = System.currentTimeMillis(),
    val highQualityUrl: String? = null,
    val lowQualityUrl: String? = null,
    val coverUrlHigh: String? = null,
    val coverUrlFast: String? = null
) {
    /**
     * Determines whether the stream URL is expired or close to expiring within [safetyBufferMs].
     * Defaults to a 60-second safety window before actual expiration.
     */
    fun isExpired(safetyBufferMs: Long = 60_000L): Boolean {
        val now = System.currentTimeMillis()
        return now + safetyBufferMs >= expiresAtMs
    }

    override fun toString(): String {
        return "ResolvedPlaybackData(videoId='$videoId', expiresAt=${Date(expiresAtMs)}, isExpired=${isExpired()}, client='$streamClient', isLossless=$isLossless)"
    }
}

/**
 * Dedicated PlaybackResolver abstraction.
 * Isolates playback stream resolution, expiration tracking, caching, and retry logic from the rest of the application.
 */
interface PlaybackResolver {
    /**
     * Resolves a playable stream for the given [videoId].
     * Returns an existing unexpired stream if available, unless [forceRefresh] is true.
     */
    suspend fun resolve(
        videoId: String,
        songId: Long? = null,
        title: String? = null,
        artist: String? = null,
        durationSec: Int = 0,
        forceRefresh: Boolean = false
    ): Result<ResolvedPlaybackData>

    /**
     * Forces invalidation of any cached stream and resolves a fresh stream URL for [videoId].
     */
    suspend fun refresh(
        videoId: String,
        songId: Long? = null,
        title: String? = null,
        artist: String? = null,
        durationSec: Int = 0
    ): Result<ResolvedPlaybackData>

    /**
     * Resolves a stream with bounded retries.
     */
    suspend fun resolveWithRetry(
        videoId: String,
        songId: Long? = null,
        title: String? = null,
        artist: String? = null,
        durationSec: Int = 0,
        maxRetries: Int = 2
    ): Result<ResolvedPlaybackData>

    /**
     * Evicts any cached stream for [videoId].
     */
    fun invalidate(videoId: String)

    /**
     * Peeks at currently cached playback data without resolving if missing or expired.
     */
    fun peek(videoId: String): ResolvedPlaybackData?
}
