package com.lin0721.linmusic.core.player

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Reconstructed track information from persistent storage upon cold launch
data class RestoredTrack(
    val songId: Long,
    val videoId: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String,
    val positionMs: Long,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val mediaItem: MediaItem
)

class PlaybackStateStore(
    private val scope: CoroutineScope,
    private val preferences: PlaybackPreferences
) {

    val playModeChanges: Flow<PlayMode> get() = preferences.playMode

    suspend fun loadPlayMode(): PlayMode = preferences.playMode.first()

    suspend fun loadQueueState(): QueueState = preferences.queueState.first()

    suspend fun loadLastTrack(): RestoredTrack? {
        val lastState = preferences.playbackState.first()
        if (lastState.songId == -1L && lastState.videoId.isBlank()) return null

        val effectiveVideoId = lastState.videoId.ifBlank {
            YouTubeIdRegistry.getVideoId(lastState.songId) ?: ""
        }

        val bundle = Bundle().apply {
            putLong("songId", lastState.songId)
            if (effectiveVideoId.isNotBlank()) putString("videoId", effectiveVideoId)
            if (lastState.album.isNotBlank()) putString("album", lastState.album)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(lastState.title)
            .setArtist(lastState.artist)
            .setAlbumTitle(lastState.album.takeIf { it.isNotBlank() })
            .setArtworkUri(lastState.coverUrl.takeIf { it.isNotBlank() }?.let { Uri.parse(it) })
            .setExtras(bundle)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(lastState.songId.toString())
            .setMediaMetadata(metadata)
            .build()

        if (lastState.songId != -1L) {
            YouTubeIdRegistry.registerMetadata(
                songId = lastState.songId,
                title = lastState.title,
                artist = lastState.artist,
                coverUrl = lastState.coverUrl,
                videoId = effectiveVideoId.takeIf { it.isNotBlank() }
            )
        }

        return RestoredTrack(
            songId = lastState.songId,
            videoId = effectiveVideoId,
            title = lastState.title,
            artist = lastState.artist,
            album = lastState.album,
            coverUrl = lastState.coverUrl,
            positionMs = lastState.lastPositionMs,
            durationMs = lastState.durationMs,
            isPlaying = lastState.isPlaying,
            mediaItem = mediaItem
        )
    }

    fun savePlayMode(mode: PlayMode) {
        scope.launch { preferences.savePlayMode(mode) }
    }

    fun saveQueue(queue: PlaybackQueue) {
        scope.launch(Dispatchers.Default) {
            preferences.saveQueueState(queue.original, queue.currentIndexInOriginal(), queue.playContext.value)
        }
    }

    suspend fun saveQueueSync(queue: PlaybackQueue) {
        preferences.saveQueueState(queue.original, queue.currentIndexInOriginal(), queue.playContext.value)
    }

    fun savePlaybackState(provider: () -> PlaybackState) {
        scope.launch { preferences.savePlaybackState(provider()) }
    }

    suspend fun savePlaybackStateSync(state: PlaybackState) {
        preferences.savePlaybackState(state)
    }
}
