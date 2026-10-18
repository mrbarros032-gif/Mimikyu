package com.lin0721.linmusic.core.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.lin0721.linmusic.core.log.AppLogger
import java.io.File

private const val TAG = "AudioCacheManager"

@UnstableApi
object AudioCacheManager {
    const val DEFAULT_MAX_CACHE_SIZE = 512L * 1024L * 1024L // 512MB default LRU cache

    @Volatile
    private var cache: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context, maxSize: Long = DEFAULT_MAX_CACHE_SIZE): SimpleCache {
        if (cache == null) {
            val cacheDir = File(context.cacheDir, "audio_cache")
            val databaseProvider = StandaloneDatabaseProvider(context)
            val evictor = LeastRecentlyUsedCacheEvictor(maxSize)
            cache = SimpleCache(cacheDir, evictor, databaseProvider)
            AppLogger.i(TAG, "Initialized Media3 SimpleCache at ${cacheDir.absolutePath} with maxSize=${maxSize / (1024 * 1024)}MB")
        }
        return cache!!
    }

    @Synchronized
    fun recreateCache(context: Context, newMaxSize: Long) {
        try {
            cache?.release()
            cache = null
            val newCache = getCache(context, newMaxSize)
            trimToSize(newCache, newMaxSize)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Cache rebuild failed", e)
        }
    }

    private fun trimToSize(cache: SimpleCache, maxSize: Long) {
        if (cache.cacheSpace <= maxSize) return
        val spans = cache.keys.flatMap { key -> cache.getCachedSpans(key) }
            .sortedBy { it.lastTouchTimestamp }
        for (span in spans) {
            if (cache.cacheSpace <= maxSize) break
            runCatching { cache.removeSpan(span) }
        }
    }

    @Synchronized
    fun clearCache(context: Context) {
        try {
            cache?.release()
            cache = null
            val cacheDir = File(context.cacheDir, "audio_cache")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
            AppLogger.i(TAG, "Audio disk cache cleared")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to clear audio disk cache", e)
        }
    }

    fun getCacheDirSize(context: Context): Long {
        val cacheDir = File(context.cacheDir, "audio_cache")
        return cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
