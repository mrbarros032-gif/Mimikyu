package com.lin0721.linmusic.feature.search.ui

import com.lin0721.linmusic.core.model.PlaylistDetail

// Playlists
sealed interface PlaylistCategoryUiState {
    data object Loading : PlaylistCategoryUiState
    data class Success(
        val playlists: List<PlaylistDetail>,
        val hasMore: Boolean,
        val total: Int,
        val isLoadingMore: Boolean = false
    ) : PlaylistCategoryUiState
    data object Empty : PlaylistCategoryUiState
    data class Error(val message: String) : PlaylistCategoryUiState
}
