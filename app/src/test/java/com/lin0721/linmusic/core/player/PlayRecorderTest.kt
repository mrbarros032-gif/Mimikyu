package com.lin0721.linmusic.core.player

import com.lin0721.linmusic.core.auth.AuthState
import com.lin0721.linmusic.core.auth.AuthUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FakePlayBatchWriter : PlayBatchWriter {
    data class BatchRecord(
        val uid: String,
        val videoId: String,
        val title: String,
        val artist: String,
        val artistId: String?,
        val album: String?,
        val thumbnailUrl: String,
        val durationSec: Long,
        val listenedSec: Long,
        val dayKey: String,
        val monthKey: String,
        val artistKey: String
    )

    val recordedBatches = mutableListOf<BatchRecord>()

    override fun writePlayBatch(
        uid: String,
        videoId: String,
        title: String,
        artist: String,
        artistId: String?,
        album: String?,
        thumbnailUrl: String,
        durationSec: Long,
        listenedSec: Long,
        dayKey: String,
        monthKey: String,
        artistKey: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        recordedBatches.add(
            BatchRecord(
                uid = uid,
                videoId = videoId,
                title = title,
                artist = artist,
                artistId = artistId,
                album = album,
                thumbnailUrl = thumbnailUrl,
                durationSec = durationSec,
                listenedSec = listenedSec,
                dayKey = dayKey,
                monthKey = monthKey,
                artistKey = artistKey
            )
        )
        onSuccess()
    }
}

class PlayRecorderTest {

    private lateinit var fakeWriter: FakePlayBatchWriter
    private lateinit var authState: AuthState

    @Before
    fun setUp() {
        fakeWriter = FakePlayBatchWriter()
        authState = AuthState()
        authState.setTestUser(AuthUser(uid = "test_user_uid_123", email = "test@example.com", displayName = "Tester"))
    }

    @Test
    fun testSongPlayedFor35SecondsRecordsExactlyOnePlay() {
        val recorder = PlayRecorder(
            authState = authState,
            batchWriter = fakeWriter
        )

        recorder.onTrackStarted(
            videoId = "dQw4w9WgXcQ",
            title = "Never Gonna Give You Up",
            artist = "Rick Astley",
            artistId = "rick_astley",
            album = "Whenever You Need Somebody",
            thumbnailUrl = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
            durationSec = 213L
        )

        // Simulate 29 seconds of playback -> should NOT record yet
        for (i in 1..29) {
            recorder.onPlaybackTick(isPlaying = true, durationMs = 213000L, deltaMs = 1000L)
            assertFalse("Should not record before 30s", recorder.sessionRecorded)
        }

        assertEquals(29000L, recorder.sessionActiveMs)
        assertEquals(0, fakeWriter.recordedBatches.size)

        // 30th second -> reaches 30s threshold and writes ONE WriteBatch
        recorder.onPlaybackTick(isPlaying = true, durationMs = 213000L, deltaMs = 1000L)
        assertTrue("Session must be marked recorded at 30s", recorder.sessionRecorded)
        assertEquals("Exactly one batch must be written", 1, fakeWriter.recordedBatches.size)

        // Continue playing for 5 more seconds (total 35s)
        for (i in 31..35) {
            recorder.onPlaybackTick(isPlaying = true, durationMs = 213000L, deltaMs = 1000L)
        }

        // Verify still exactly ONE batch was recorded (no duplicate writes)
        assertEquals("Must not write duplicate batches for the same session", 1, fakeWriter.recordedBatches.size)

        val batch = fakeWriter.recordedBatches.first()
        val expectedDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val expectedMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        assertEquals("test_user_uid_123", batch.uid)
        assertEquals("dQw4w9WgXcQ", batch.videoId)
        assertEquals("Never Gonna Give You Up", batch.title)
        assertEquals("Rick Astley", batch.artist)
        assertEquals("rick_astley", batch.artistId)
        assertEquals("Whenever You Need Somebody", batch.album)
        assertEquals(213L, batch.durationSec)
        assertEquals(30L, batch.listenedSec)
        assertEquals(expectedDay, batch.dayKey)
        assertEquals(expectedMonth, batch.monthKey)
        assertEquals("rick_astley", batch.artistKey)
    }

    @Test
    fun testPauseAndSeekJumpsDoNotRecordPlayUnder30Seconds() {
        val recorder = PlayRecorder(
            authState = authState,
            batchWriter = fakeWriter
        )

        recorder.onTrackStarted(
            videoId = "short_play_video",
            title = "Short Play Song",
            artist = "Artist A",
            durationSec = 200L
        )

        // Play for 10 seconds
        for (i in 1..10) {
            recorder.onPlaybackTick(isPlaying = true, durationMs = 200000L, deltaMs = 1000L)
        }
        assertEquals(10000L, recorder.sessionActiveMs)

        // Paused for 40 seconds (user went away or paused) -> must NOT accumulate
        for (i in 1..40) {
            recorder.onPlaybackTick(isPlaying = false, durationMs = 200000L, deltaMs = 1000L)
        }
        assertEquals("Paused time must not accumulate", 10000L, recorder.sessionActiveMs)

        // User seeks to end or anywhere, but plays only 10 more seconds (total 20s active)
        for (i in 1..10) {
            recorder.onPlaybackTick(isPlaying = true, durationMs = 200000L, deltaMs = 1000L)
        }
        assertEquals(20000L, recorder.sessionActiveMs)
        assertFalse("Under 30 seconds must not record a play", recorder.sessionRecorded)
        assertEquals(0, fakeWriter.recordedBatches.size)
    }

    @Test
    fun testSongUnder60SecondsRecordsAt50Percent() {
        val recorder = PlayRecorder(
            authState = authState,
            batchWriter = fakeWriter
        )

        // 40 second song -> 50% is 20s
        recorder.onTrackStarted(
            videoId = "short_song",
            title = "Jingle",
            artist = "Commercial",
            durationSec = 40L
        )

        // 19 seconds of play -> not recorded yet
        for (i in 1..19) {
            recorder.onPlaybackTick(isPlaying = true, durationMs = 40000L, deltaMs = 1000L)
            assertFalse(recorder.sessionRecorded)
        }
        assertEquals(0, fakeWriter.recordedBatches.size)

        // 20th second (50% of 40s) -> records!
        recorder.onPlaybackTick(isPlaying = true, durationMs = 40000L, deltaMs = 1000L)
        assertTrue("40s song should record at 20s (50%)", recorder.sessionRecorded)
        assertEquals(1, fakeWriter.recordedBatches.size)
        assertEquals(20L, fakeWriter.recordedBatches.first().listenedSec)
    }

    @Test
    fun testTrackTransitionResetsSessionForNextSong() {
        val recorder = PlayRecorder(
            authState = authState,
            batchWriter = fakeWriter
        )

        // Song 1 plays for 35s
        recorder.onTrackStarted("song_1", "Song 1", "Artist 1", durationSec = 180L)
        for (i in 1..35) {
            recorder.onPlaybackTick(isPlaying = true, durationMs = 180000L, deltaMs = 1000L)
        }
        assertTrue(recorder.sessionRecorded)
        assertEquals(1, fakeWriter.recordedBatches.size)

        // Transition to Song 2
        recorder.resetSession()
        assertFalse(recorder.sessionRecorded)
        assertEquals(0L, recorder.sessionActiveMs)

        recorder.onTrackStarted("song_2", "Song 2", "Artist 2", durationSec = 180L)
        assertFalse(recorder.sessionRecorded)

        // Song 2 plays for only 15s then skips
        for (i in 1..15) {
            recorder.onPlaybackTick(isPlaying = true, durationMs = 180000L, deltaMs = 1000L)
        }
        assertFalse("Song 2 under 30s must not record", recorder.sessionRecorded)
        assertEquals("Still only 1 batch total from song 1", 1, fakeWriter.recordedBatches.size)
    }
}
