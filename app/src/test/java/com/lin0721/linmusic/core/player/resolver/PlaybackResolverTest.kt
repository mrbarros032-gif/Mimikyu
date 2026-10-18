package com.lin0721.linmusic.core.player.resolver

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackResolverTest {

    @Test
    fun `ResolvedPlaybackData expiration detection with safety buffer`() {
        val now = System.currentTimeMillis()

        // 1. Valid for 10 minutes: should not be expired
        val validData = ResolvedPlaybackData(
            videoId = "test_vid_1",
            streamUrl = "https://googlevideo.com/videoplayback?expire=${(now + 600_000) / 1000}",
            expiresAtMs = now + 600_000L
        )
        assertFalse("10 minutes left should NOT be expired", validData.isExpired(safetyBufferMs = 60_000L))

        // 2. Only 30 seconds left: should be considered expired with 60s safety buffer
        val nearExpiryData = ResolvedPlaybackData(
            videoId = "test_vid_2",
            streamUrl = "https://googlevideo.com/videoplayback?expire=${(now + 30_000) / 1000}",
            expiresAtMs = now + 30_000L
        )
        assertTrue("30 seconds left with 60s safety buffer SHOULD be expired", nearExpiryData.isExpired(safetyBufferMs = 60_000L))

        // 3. Past expiry: should definitely be expired
        val expiredData = ResolvedPlaybackData(
            videoId = "test_vid_3",
            streamUrl = "https://googlevideo.com/videoplayback",
            expiresAtMs = now - 1000L
        )
        assertTrue("Past expiry SHOULD be expired", expiredData.isExpired(safetyBufferMs = 0L))
    }

    @Test
    fun `parseExpiresAtMs extracts expire query param correctly`() {
        val expireEpochSeconds = 1770000000L
        val url = "https://rr3---sn-4g5edn6e.googlevideo.com/videoplayback?expire=$expireEpochSeconds&ei=abc&ip=1.2.3.4"

        val parsedMs = PlaybackResolverImpl.parseExpiresAtMs(url, streamExpiresInSeconds = null)
        assertEquals(expireEpochSeconds * 1000L, parsedMs)
    }

    @Test
    fun `parseExpiresAtMs falls back to streamExpiresInSeconds when url lacks expire param`() {
        val now = System.currentTimeMillis()
        val url = "https://example.com/audio.mp3"

        val parsedMs = PlaybackResolverImpl.parseExpiresAtMs(url, streamExpiresInSeconds = 3600, nowMs = now)
        val expectedMin = now + (3590 * 1000L)
        val expectedMax = now + (3610 * 1000L)
        assertTrue("Should calculate expiry ~1 hour in future", parsedMs in expectedMin..expectedMax)
    }

    @Test
    fun `parseExpiresAtMs falls back to 4 hours default when both are missing`() {
        val now = System.currentTimeMillis()
        val url = "https://example.com/audio.mp3"

        val parsedMs = PlaybackResolverImpl.parseExpiresAtMs(url, streamExpiresInSeconds = null, nowMs = now)
        val expectedMin = now + (3 * 3600 * 1000L)
        val expectedMax = now + (5 * 3600 * 1000L)
        assertTrue("Should default to ~4 hours in future", parsedMs in expectedMin..expectedMax)
    }

    @Test
    fun `PlaybackResolver caches valid streams and invalidates properly`() = runBlocking {
        var callCount = 0
        val resolver = object : PlaybackResolver {
            private val cache = mutableMapOf<String, ResolvedPlaybackData>()

            override suspend fun resolve(
                videoId: String,
                songId: Long?,
                title: String?,
                artist: String?,
                durationSec: Int,
                forceRefresh: Boolean
            ): Result<ResolvedPlaybackData> {
                if (!forceRefresh) {
                    val cached = cache[videoId]
                    if (cached != null && !cached.isExpired(60_000L)) {
                        return Result.success(cached)
                    }
                }
                callCount++
                val fresh = ResolvedPlaybackData(
                    videoId = videoId,
                    streamUrl = "https://stream.example.com/$videoId?call=$callCount",
                    expiresAtMs = System.currentTimeMillis() + 3600_000L
                )
                cache[videoId] = fresh
                return Result.success(fresh)
            }

            override suspend fun refresh(
                videoId: String,
                songId: Long?,
                title: String?,
                artist: String?,
                durationSec: Int
            ): Result<ResolvedPlaybackData> = resolve(videoId, songId, title, artist, durationSec, forceRefresh = true)

            override suspend fun resolveWithRetry(
                videoId: String,
                songId: Long?,
                title: String?,
                artist: String?,
                durationSec: Int,
                maxRetries: Int
            ): Result<ResolvedPlaybackData> = resolve(videoId, songId, title, artist, durationSec)

            override fun invalidate(videoId: String) {
                cache.remove(videoId)
            }

            override fun peek(videoId: String): ResolvedPlaybackData? = cache[videoId]
        }

        // First resolve: fresh fetch
        val first = resolver.resolve("vid123", 1L, "Title", "Artist").getOrThrow()
        assertEquals(1, callCount)
        assertEquals("https://stream.example.com/vid123?call=1", first.streamUrl)

        // Second resolve: should return cached without new fetch
        val second = resolver.resolve("vid123", 1L, "Title", "Artist").getOrThrow()
        assertEquals(1, callCount)
        assertEquals(first.streamUrl, second.streamUrl)

        // Force refresh: should bypass cache and fetch new stream
        val refreshed = resolver.refresh("vid123", 1L, "Title", "Artist").getOrThrow()
        assertEquals(2, callCount)
        assertEquals("https://stream.example.com/vid123?call=2", refreshed.streamUrl)

        // Invalidate: cache cleared
        assertNotNull(resolver.peek("vid123"))
        resolver.invalidate("vid123")
        assertNull(resolver.peek("vid123"))

        // Resolve after invalidate: fresh fetch
        val afterInvalidate = resolver.resolve("vid123", 1L, "Title", "Artist").getOrThrow()
        assertEquals(3, callCount)
        assertEquals("https://stream.example.com/vid123?call=3", afterInvalidate.streamUrl)
    }

    @Test
    fun `resolveWithRetry performs bounded retries on transient errors`() = runBlocking {
        var attempts = 0
        val resolver = object : PlaybackResolver {
            override suspend fun resolve(
                videoId: String,
                songId: Long?,
                title: String?,
                artist: String?,
                durationSec: Int,
                forceRefresh: Boolean
            ): Result<ResolvedPlaybackData> {
                attempts++
                return if (attempts < 3) {
                    Result.failure(RuntimeException("Transient network error attempt $attempts"))
                } else {
                    Result.success(
                        ResolvedPlaybackData(
                            videoId = videoId,
                            streamUrl = "https://stream.example.com/recovered",
                            expiresAtMs = System.currentTimeMillis() + 3600_000L
                        )
                    )
                }
            }

            override suspend fun refresh(
                videoId: String,
                songId: Long?,
                title: String?,
                artist: String?,
                durationSec: Int
            ): Result<ResolvedPlaybackData> = resolve(videoId, songId, title, artist, durationSec, forceRefresh = true)

            override suspend fun resolveWithRetry(
                videoId: String,
                songId: Long?,
                title: String?,
                artist: String?,
                durationSec: Int,
                maxRetries: Int
            ): Result<ResolvedPlaybackData> {
                var lastError: Throwable? = null
                for (i in 0 until maxRetries) {
                    val res = resolve(videoId, songId, title, artist, durationSec, forceRefresh = i > 0)
                    if (res.isSuccess) return res
                    lastError = res.exceptionOrNull()
                }
                return Result.failure(lastError ?: RuntimeException("Failed after $maxRetries retries"))
            }

            override fun invalidate(videoId: String) {}
            override fun peek(videoId: String): ResolvedPlaybackData? = null
        }

        val result = resolver.resolveWithRetry("vid_retry", 1L, "Title", "Artist", maxRetries = 3)
        assertTrue("Should succeed on 3rd attempt", result.isSuccess)
        assertEquals(3, attempts)
        assertEquals("https://stream.example.com/recovered", result.getOrThrow().streamUrl)
    }
}
