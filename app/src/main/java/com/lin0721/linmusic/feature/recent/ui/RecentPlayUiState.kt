package com.lin0721.linmusic.feature.recent.ui

import com.lin0721.linmusic.feature.recent.domain.RecentAlbum
import com.lin0721.linmusic.feature.recent.domain.RecentPlaylist
import com.lin0721.linmusic.feature.recent.domain.RecentSong

// Recently Played of
enum class RecentTab(val label: String) {
    SONG("Songs"),
    PLAYLIST("Playlists"),
    ALBUM("Albums")
}

// Recently Played UI
sealed interface RecentPlayUiState {
    data object Loading : RecentPlayUiState

    // ， Tab
    data class Success(
        val songs: List<RecentSong>,
        val playlists: List<RecentPlaylist>,
        val albums: List<RecentAlbum>
    ) : RecentPlayUiState

    data class Error(val message: String) : RecentPlayUiState
}
