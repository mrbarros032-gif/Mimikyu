package com.lin0721.linmusic.core.songlike

import com.google.firebase.Timestamp
import com.lin0721.linmusic.core.model.Album
import com.lin0721.linmusic.core.model.Artist
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.player.QueueItem
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class LikedSong(
    val videoId: String = "",
    val title: String = "",
    val artist: String = "",
    val artistId: String? = null,
    val album: String? = null,
    val durationSec: Long = 0L,
    val thumbnailUrl: String = "",
    @Transient
    val likedAt: Timestamp? = null
) {
    fun toQueueItem(): QueueItem {
        val songId = YouTubeIdRegistry.register(
            videoId = videoId,
            title = title,
            artist = artist,
            durationSec = durationSec.toInt(),
            album = album.orEmpty(),
            coverUrl = thumbnailUrl
        )
        return QueueItem(
            songId = songId,
            title = title,
            artist = artist,
            coverUrl = thumbnailUrl,
            videoId = videoId,
            album = album,
            durationSec = durationSec.toInt()
        )
    }

    fun toTrack(): Track {
        val songId = YouTubeIdRegistry.register(
            videoId = videoId,
            title = title,
            artist = artist,
            durationSec = durationSec.toInt(),
            album = album.orEmpty(),
            coverUrl = thumbnailUrl
        )
        return Track(
            id = songId,
            name = title,
            ar = listOf(Artist(id = artistId?.toLongOrNull() ?: 0L, name = artist)),
            al = Album(id = 0L, name = album.orEmpty(), picUrl = thumbnailUrl),
            dt = durationSec * 1000L
        )
    }
}

fun Track.toLikedSong(): LikedSong? {
    val vid = YouTubeIdRegistry.getVideoId(id)?.takeIf { it.isNotBlank() } ?: return null
    return LikedSong(
        videoId = vid,
        title = name,
        artist = ar.firstOrNull()?.name ?: "",
        artistId = ar.firstOrNull()?.id?.toString(),
        album = al.name.takeIf { it.isNotBlank() },
        durationSec = dt / 1000L,
        thumbnailUrl = al.picUrl
    )
}
