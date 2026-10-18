package com.lin0721.linmusic.feature.recent.domain

import com.lin0721.linmusic.core.model.Track

/**
 * Playback history record backed by Firestore plays collection.
 * Contains both document ID (for LazyColumn keys) and videoId (for YouTube Music streaming).
 */
data class RecentSong(
    val id: String = track.id.toString(),
    val videoId: String = track.id.toString(),
    val title: String = track.name,
    val artist: String = track.ar.joinToString(" / ") { it.name },
    val album: String? = track.al.name,
    val coverUrl: String = track.al.picUrl,
    val durationSec: Long = track.dt / 1000L,
    val listenedSec: Long = 0L,
    val playTime: Long = System.currentTimeMillis(),
    val dayKey: String = "",
    val playedAtText: String = "",
    val track: Track
)

data class RecentPlaylist(
    val id: Long,
    val name: String,
    val coverUrl: String,
    val creatorName: String,
    val playTime: Long,
    val playedAtText: String
)

data class RecentAlbum(
    val id: Long,
    val name: String,
    val coverUrl: String,
    val artistName: String,
    val playTime: Long,
    val playedAtText: String
)
