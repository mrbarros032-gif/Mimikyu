package com.lin0721.linmusic.core.player

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object CrossfadePolicy {

    // MediaItem extras，、
    const val EXTRA_CROSSFADE_MS = "crossfadeMs"
    const val EXTRA_CROSSFADE_START_MS = "crossfadeStartMs"

    const val MIN_DURATION_MS = 500
    const val MAX_DURATION_MS = 12_000
    const val DURATION_STEP_MS = 500
    const val DEFAULT_DURATION_MS = 3000

    private const val BASE_PREFETCH_WINDOW_MS = 8000L

    // Next
    const val ANALYSIS_WINDOW_MS = 35_000L

    fun normalizeDurationMs(durationMs: Int): Int {
        val clamped = durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
        return (clamped + DURATION_STEP_MS / 2) / DURATION_STEP_MS * DURATION_STEP_MS
    }

    // ： of ，
    fun autoFadeMs(durationMs: Int, effectiveDurationMs: Long): Long {
        if (effectiveDurationMs <= 0L) return 0L
        return minOf(normalizeDurationMs(durationMs).toLong(), effectiveDurationMs / 2)
    }

    // ，
    fun triggerPositionMs(effectiveEndMs: Long, fadeMs: Long): Long =
        (effectiveEndMs - fadeMs).coerceAtLeast(0L)

    // ；、、Play
    fun fadeMsFor(
        autoTransition: Boolean,
        enabled: Boolean,
        isPlaying: Boolean,
        startPositionMs: Long,
        autoFadeMs: Long
    ): Long {
        if (!autoTransition || !enabled || !isPlaying || startPositionMs > 0L) return 0L
        return autoFadeMs.coerceAtLeast(0L)
    }

    fun prefetchWindowMs(enabled: Boolean): Long =
        if (enabled) ANALYSIS_WINDOW_MS else BASE_PREFETCH_WINDOW_MS

    // ： 1，
    fun fadeOutGain(progress: Float, startGain: Float): Float =
        startGain * cos(progress.coerceIn(0f, 1f) * PI / 2).toFloat()

    fun fadeInGain(progress: Float): Float =
        sin(progress.coerceIn(0f, 1f) * PI / 2).toFloat()
}
