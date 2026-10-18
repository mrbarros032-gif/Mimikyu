package com.lin0721.linmusic.core.player

import androidx.media3.common.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStateTest {

    @Test
    fun `PlaybackState carries videoId, album, and isPlaying with correct defaults`() {
        val defaultState = PlaybackState()
        assertEquals(-1L, defaultState.songId)
        assertEquals("", defaultState.title)
        assertEquals(0L, defaultState.lastPositionMs)
        assertEquals(0L, defaultState.durationMs)
        assertEquals("", defaultState.videoId)
        assertEquals("", defaultState.album)
        assertEquals(false, defaultState.isPlaying)

        val customState = PlaybackState(
            songId = 1001L,
            title = "Test Song",
            artist = "Test Artist",
            coverUrl = "https://example.com/cover.jpg",
            lastPositionMs = 45000L,
            durationMs = 180000L,
            videoId = "yt_test_vid",
            album = "Test Album",
            isPlaying = true
        )
        assertEquals(1001L, customState.songId)
        assertEquals("yt_test_vid", customState.videoId)
        assertEquals("Test Album", customState.album)
        assertEquals(45000L, customState.lastPositionMs)
        assertEquals(180000L, customState.durationMs)
        assertTrue(customState.isPlaying)
    }

    @Test
    fun `RestoredTrack carries videoId, album, and isPlaying for process death restoration`() {
        val mediaItem = MediaItem.Builder()
            .setMediaId("2002")
            .build()

        val restored = RestoredTrack(
            songId = 2002L,
            videoId = "recovered_vid_abc",
            title = "Bohemian Rhapsody",
            artist = "Queen",
            album = "Greatest Hits",
            coverUrl = "https://example.com/art.jpg",
            positionMs = 30000L,
            durationMs = 240000L,
            isPlaying = false,
            mediaItem = mediaItem
        )
        assertEquals(2002L, restored.songId)
        assertEquals("recovered_vid_abc", restored.videoId)
        assertEquals("Bohemian Rhapsody", restored.title)
        assertEquals("Queen", restored.artist)
        assertEquals("Greatest Hits", restored.album)
        assertEquals(30000L, restored.positionMs)
        assertEquals(240000L, restored.durationMs)
        assertFalse(restored.isPlaying)
        assertEquals("2002", restored.mediaItem.mediaId)
    }
}
