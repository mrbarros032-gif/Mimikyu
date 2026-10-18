package com.lin0721.linmusic.core.metadata

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Local cache for enriched metadata keyed by songId (YouTube videoId or track ID).
 * Features fast in-memory caching and persistent disk storage.
 */
class MetadataCache(private val context: Context) {

    private val memoryCache = ConcurrentHashMap<String, EnrichedMetadata>()
    private val queryMemoryCache = ConcurrentHashMap<String, EnrichedMetadata>()
    private val json = Json { ignoreUnknownKeys = true }
    private val cacheDir: File by lazy {
        File(context.filesDir, "itunes_metadata_cache").apply {
            if (!exists()) mkdirs()
        }
    }

    private fun sanitizeKey(key: String): String {
        return key.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }

    private fun queryKey(title: String, artist: String): String {
        return "${cleanMetadataText(title).lowercase()}:${cleanMetadataText(artist).lowercase()}"
    }

    fun get(songId: String): EnrichedMetadata? {
        if (songId.isBlank()) return null

        // 1. Fast in-memory lookup
        memoryCache[songId]?.let { return it }

        // 2. Persistent disk lookup
        return try {
            val file = File(cacheDir, "${sanitizeKey(songId)}.json")
            if (file.exists() && file.isFile) {
                val content = file.readText()
                val metadata = json.decodeFromString<EnrichedMetadata>(content)
                memoryCache[songId] = metadata
                if (metadata.title.isNotBlank() && metadata.artist.isNotBlank()) {
                    queryMemoryCache[queryKey(metadata.title, metadata.artist)] = metadata
                }
                metadata
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.tag("MetadataCache").w("Failed to read disk cache for $songId: ${e.message}")
            null
        }
    }

    fun getByQuery(title: String, artist: String): EnrichedMetadata? {
        val qk = queryKey(title, artist)
        if (qk == ":") return null

        queryMemoryCache[qk]?.let { return it }

        return try {
            val file = File(cacheDir, "q_${sanitizeKey(qk)}.json")
            if (file.exists() && file.isFile) {
                val content = file.readText()
                val metadata = json.decodeFromString<EnrichedMetadata>(content)
                queryMemoryCache[qk] = metadata
                metadata
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.tag("MetadataCache").w("Failed to read disk query cache for $qk: ${e.message}")
            null
        }
    }

    suspend fun put(metadata: EnrichedMetadata) = withContext(Dispatchers.IO) {
        if (metadata.songId.isNotBlank()) {
            memoryCache[metadata.songId] = metadata
            try {
                val file = File(cacheDir, "${sanitizeKey(metadata.songId)}.json")
                val content = json.encodeToString(metadata)
                file.writeText(content)
            } catch (e: Exception) {
                Timber.tag("MetadataCache").w("Failed to write disk cache for ${metadata.songId}: ${e.message}")
            }
        }

        if (metadata.title.isNotBlank() && metadata.artist.isNotBlank()) {
            val qk = queryKey(metadata.title, metadata.artist)
            queryMemoryCache[qk] = metadata
            try {
                val file = File(cacheDir, "q_${sanitizeKey(qk)}.json")
                val content = json.encodeToString(metadata)
                file.writeText(content)
            } catch (e: Exception) {
                Timber.tag("MetadataCache").w("Failed to write query disk cache for $qk: ${e.message}")
            }
        }
    }

    fun clear() {
        memoryCache.clear()
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Timber.tag("MetadataCache").w("Failed to clear disk cache: ${e.message}")
        }
    }
}
