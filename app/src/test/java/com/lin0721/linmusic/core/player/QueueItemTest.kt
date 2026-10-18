package com.lin0721.linmusic.core.player

import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueItemTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `effectiveVideoId prioritizes explicit videoId then falls back to YouTubeIdRegistry`() {
        // 1. Explicit videoId takes priority
        val explicitItem = QueueItem(
            songId = 1001L,
            title = "Test Song",
            artist = "Test Artist",
            coverUrl = "https://example.com/cover.jpg",
            videoId = "explicit_vid_123"
        )
        assertEquals("explicit_vid_123", explicitItem.effectiveVideoId())

        // 2. Fallback to YouTubeIdRegistry when videoId is null
        val registrySongId = YouTubeIdRegistry.register(
            videoId = "registry_vid_456",
            title = "Registry Song",
            artist = "Registry Artist"
        )
        assertTrue(registrySongId > 0L)

        val registryFallbackItem = QueueItem(
            songId = registrySongId,
            title = "Registry Song",
            artist = "Registry Artist",
            coverUrl = "https://example.com/cover2.jpg",
            videoId = null
        )
        assertEquals("registry_vid_456", registryFallbackItem.effectiveVideoId())

        // 3. Null when neither is available
        val unknownItem = QueueItem(
            songId = 99999999L,
            title = "Unknown Song",
            artist = "Unknown Artist",
            coverUrl = "https://example.com/cover3.jpg",
            videoId = null
        )
        assertNull(unknownItem.effectiveVideoId())
    }

    @Test
    fun `QueueItem serialization preserves videoId and never persists temporary stream URLs`() {
        val item = QueueItem(
            songId = 7777L,
            title = "Bohemian Rhapsody",
            artist = "Queen",
            coverUrl = "https://example.com/queen.jpg",
            videoId = "fJ9rUzIMcZQ",
            album = "A Night at the Opera",
            durationSec = 354
        )

        val serializedJson = json.encodeToString(item)

        // Ensure serialized string does NOT contain any temporary stream URL
        assertFalse("JSON must not contain googlevideo or stream URLs", serializedJson.contains("googlevideo"))
        assertFalse("JSON must not contain videoplayback", serializedJson.contains("videoplayback"))

        // Ensure persistent identity videoId IS contained
        assertTrue("JSON must contain videoId", serializedJson.contains("\"videoId\":\"fJ9rUzIMcZQ\""))

        // Deserialize and verify integrity
        val restoredItem = json.decodeFromString<QueueItem>(serializedJson)
        assertEquals(7777L, restoredItem.songId)
        assertEquals("Bohemian Rhapsody", restoredItem.title)
        assertEquals("Queen", restoredItem.artist)
        assertEquals("fJ9rUzIMcZQ", restoredItem.videoId)
        assertEquals("A Night at the Opera", restoredItem.album)
        assertEquals(354, restoredItem.durationSec)
        assertEquals("fJ9rUzIMcZQ", restoredItem.effectiveVideoId())
    }

    @Test
    fun `QueueItem copy with updated metadata works seamlessly`() {
        val initial = QueueItem(
            songId = 1234L,
            title = "Initial Title",
            artist = "Initial Artist",
            coverUrl = "https://example.com/art.jpg"
        )
        assertNull(initial.videoId)

        val updated = initial.copy(
            videoId = "fresh_vid_999",
            album = "New Album",
            durationSec = 240
        )
        assertEquals("fresh_vid_999", updated.videoId)
        assertEquals("New Album", updated.album)
        assertEquals(240, updated.durationSec)
        assertEquals("fresh_vid_999", updated.effectiveVideoId())
    }
}
