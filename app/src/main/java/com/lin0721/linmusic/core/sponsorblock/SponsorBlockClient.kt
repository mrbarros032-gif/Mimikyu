package com.lin0721.linmusic.core.sponsorblock

import com.lin0721.linmusic.core.log.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private const val TAG = "SponsorBlockClient"

@Serializable
data class SponsorSegmentResponse(
    val category: String = "",
    val actionType: String = "skip",
    val segment: List<Double> = emptyList(),
    val UUID: String = ""
)

data class SponsorSegment(
    val startMs: Long,
    val endMs: Long,
    val category: String = "music_offtopic"
)

/**
 * SponsorBlock API client for querying crowdsourced non-music segments (intros, outros, spoken bits).
 * Results are cached in-memory per video ID so repeat plays do not re-query.
 */
open class SponsorBlockClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // Cache segments per video ID locally so repeat plays don't re-query
    private val cache = ConcurrentHashMap<String, List<SponsorSegment>>()

    open suspend fun getSkipSegments(videoId: String): List<SponsorSegment> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext emptyList()

        // Return cached lookup (even if emptyList) to prevent repeat queries
        cache[videoId]?.let { return@withContext it }

        val categoriesParam = listOf(
            "music_offtopic",
            "intro",
            "outro",
            "preview",
            "filler",
            "sponsor",
            "selfpromo",
            "interaction"
        ).joinToString("&") { "category=$it" }

        val url = "https://sponsor.ajay.app/api/skipSegments?videoID=$videoId&$categoriesParam&actionType=skip"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mimikyu/1.0 (Android)")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    // 404 or any other error means no community segments exist for this video
                    cache[videoId] = emptyList()
                    return@withContext emptyList()
                }

                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    cache[videoId] = emptyList()
                    return@withContext emptyList()
                }

                val list = json.decodeFromString<List<SponsorSegmentResponse>>(body)
                val rawSegments = list.mapNotNull { item ->
                    val startSec = item.segment.getOrNull(0) ?: return@mapNotNull null
                    val endSec = item.segment.getOrNull(1) ?: return@mapNotNull null
                    val startMs = (startSec * 1000).toLong()
                    val endMs = (endSec * 1000).toLong()
                    if (endMs > startMs) {
                        SponsorSegment(
                            startMs = startMs,
                            endMs = endMs,
                            category = item.category.ifBlank { "music_offtopic" }
                        )
                    } else null
                }

                // Sort and merge overlapping or contiguous segments
                val sortedSegments = rawSegments.sortedBy { it.startMs }
                val mergedSegments = mutableListOf<SponsorSegment>()
                for (seg in sortedSegments) {
                    if (mergedSegments.isEmpty()) {
                        mergedSegments.add(seg)
                    } else {
                        val last = mergedSegments.last()
                        if (seg.startMs <= last.endMs + 300L) {
                            mergedSegments[mergedSegments.lastIndex] = last.copy(
                                endMs = maxOf(last.endMs, seg.endMs)
                            )
                        } else {
                            mergedSegments.add(seg)
                        }
                    }
                }

                cache[videoId] = mergedSegments
                AppLogger.d(TAG, "Fetched ${mergedSegments.size} SponsorBlock segments for $videoId: $mergedSegments")
                mergedSegments
            }
        } catch (e: Exception) {
            AppLogger.d(TAG, "SponsorBlock fetch skipped or timed out for $videoId: ${e.message}")
            cache[videoId] = emptyList()
            emptyList()
        }
    }

    fun clearCache() {
        cache.clear()
    }
}
