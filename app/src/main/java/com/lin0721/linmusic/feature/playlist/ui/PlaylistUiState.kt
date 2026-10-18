package com.lin0721.linmusic.feature.playlist.ui

import com.lin0721.linmusic.feature.home.data.DailySong
import com.lin0721.linmusic.core.model.PlaylistDetail
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.userplaylist.UserPlaylist

// Playlists/AlbumsDetails UI
sealed interface PlaylistUiState {
    data object Loading : PlaylistUiState

    data class Success(
        val playlist: PlaylistDetail,
        val recommendedSongs: List<Track> = emptyList(),
        val isSubscribed: Boolean = false,
        val hasMoreTracks: Boolean = false,
        val isLoadingMoreTracks: Boolean = false,
        val trackPlayCounts: Map<Long, Int> = emptyMap(),
        val youMightLikeItems: List<CuratedMixItem> = emptyList()
    ) : PlaylistUiState
    data class Error(val message: String) : PlaylistUiState
}

// Daily History（Daily Recommendations/Listening Rank） of ，
data class HistoryRecommendState(
    val dates: List<String> = emptyList(),
    val datesLoading: Boolean = false,
    val songs: List<DailySong> = emptyList(),
    val selectedDate: String? = null,
    val songsLoading: Boolean = false
)

// "Add to Playlist" of
data class PlaylistImportState(
    val items: List<UserPlaylist> = emptyList(),
    val isLoading: Boolean = false
)
