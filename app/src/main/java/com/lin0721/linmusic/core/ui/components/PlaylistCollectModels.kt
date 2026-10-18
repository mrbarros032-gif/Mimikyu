package com.lin0721.linmusic.core.ui.components

// "Add to Playlist"Save of ， playlist、artist  of SavePlaylists UI 。

data class PlaylistCollectItem(
    val playlistId: Long,
    val playlistName: String,
    val coverUrl: String,
    val isInitiallyContains: Boolean,
    var isContains: Boolean
)

data class PlaylistCollectState(
    val songId: Long = -1L,
    val collectItems: List<PlaylistCollectItem> = emptyList(),
    val isLoading: Boolean = false
)
