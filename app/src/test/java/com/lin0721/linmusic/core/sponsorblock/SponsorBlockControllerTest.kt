package com.lin0721.linmusic.core.sponsorblock

import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SponsorBlockControllerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // Mock client returning configured segments without real network calls
    private class FakeSponsorBlockClient(
        private val segmentMap: Map<String, List<SponsorSegment>>
    ) : SponsorBlockClient() {
        override suspend fun getSkipSegments(videoId: String): List<SponsorSegment> {
            return segmentMap[videoId] ?: emptyList()
        }
    }

    @Before
    fun setup() {
        // Register mock song ID to video ID
        YouTubeIdRegistry.register(
            videoId = "test_video_123",
            title = "Test Song With Intro",
            artist = "Test Artist"
        )
    }

    @Test
    fun testAutoSkipIntroSegment() = testScope.runTest {
        val songId = YouTubeIdRegistry.register(videoId = "test_video_123")
        val introSegment = SponsorSegment(startMs = 0L, endMs = 15000L, category = "music_offtopic")
        val outroSegment = SponsorSegment(startMs = 180000L, endMs = 195000L, category = "music_offtopic")

        val client = FakeSponsorBlockClient(
            mapOf("test_video_123" to listOf(introSegment, outroSegment))
        )

        var seekedPos = -1L
        val notices = mutableListOf<String>()

        val controller = SponsorBlockController(
            scope = this,
            sponsorBlockClient = client,
            isEnabled = { true },
            onSeekRequested = { seekedPos = it },
            onSkippedNotice = { notices.add(it) }
        )

        // 1. When track starts at 0ms, it should detect the intro and skip to 15000ms
        controller.onTrackChanged(songId, initialPosMs = 0L)
        testScheduler.advanceUntilIdle()

        assertEquals(15000L, seekedPos)
        assertEquals(listOf("Skipped non-music intro"), notices)

        // 2. Playback advances to 100000ms (normal music) -> no skip
        seekedPos = -1L
        controller.onProgressTick(100000L)
        assertEquals(-1L, seekedPos)

        // 3. Playback reaches outro (180500ms) -> skips to 195000ms
        controller.onProgressTick(180500L)
        assertEquals(195000L, seekedPos)
        assertEquals(2, notices.size)
        assertEquals("Skipped non-music section", notices[1])

        // 4. Repeated progress inside already-skipped segment shouldn't skip again
        seekedPos = -1L
        controller.onProgressTick(181000L)
        assertEquals(-1L, seekedPos)
    }

    @Test
    fun testDisabledControllerDoesNotSkip() = testScope.runTest {
        val songId = YouTubeIdRegistry.register(videoId = "test_video_123")
        val client = FakeSponsorBlockClient(
            mapOf("test_video_123" to listOf(SponsorSegment(0L, 10000L, "music_offtopic")))
        )

        var seekedPos = -1L
        val controller = SponsorBlockController(
            scope = this,
            sponsorBlockClient = client,
            isEnabled = { false },
            onSeekRequested = { seekedPos = it },
            onSkippedNotice = {}
        )

        controller.onTrackChanged(songId, 0L)
        testScheduler.advanceUntilIdle()

        assertEquals(-1L, seekedPos)
    }
}
