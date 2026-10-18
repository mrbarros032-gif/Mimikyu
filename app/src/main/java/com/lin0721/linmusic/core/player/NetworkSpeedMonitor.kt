package com.lin0721.linmusic.core.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter

/**
 * Real-time network throughput measurement backed by ExoPlayer's DefaultBandwidthMeter.
 *
 * Defaults to an optimistic 2 Mbps estimate on startup so High Quality (Opus 251 / AAC 140)
 * is ALWAYS attempted first unless real measured speeds prove the connection is choking.
 */
@UnstableApi
object NetworkSpeedMonitor {
    @Volatile
    private var bandwidthMeter: DefaultBandwidthMeter? = null

    @Synchronized
    fun getMeter(context: Context): DefaultBandwidthMeter {
        return bandwidthMeter ?: synchronized(this) {
            bandwidthMeter ?: DefaultBandwidthMeter.Builder(context.applicationContext)
                // Assume fast internet on startup (2 Mbps) so it ALWAYS tries High Quality first!
                .setInitialBitrateEstimate(2_000_000L)
                .build().also { bandwidthMeter = it }
        }
    }

    /**
     * Only returns true if REAL measured speed drops below 300 kbps (37.5 KB/s).
     * High Quality requires ~160 kbps; setting threshold to 300 kbps ensures normal 4G/5G
     * stays in High Quality.
     */
    fun isVerySlowInternet(context: Context): Boolean {
        val currentSpeedBps = getMeter(context).bitrateEstimate
        return currentSpeedBps in 1..300_000L
    }

    /**
     * Returns true once measured speed recovers above 600 kbps (75 KB/s).
     */
    fun isFastInternetRecovered(context: Context): Boolean {
        val currentSpeedBps = getMeter(context).bitrateEstimate
        return currentSpeedBps >= 600_000L
    }

    fun currentBitrateEstimate(context: Context): Long {
        return getMeter(context).bitrateEstimate
    }
}
