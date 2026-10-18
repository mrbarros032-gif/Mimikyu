package com.lin0721.linmusic.feature.library.ui

// UI
sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    // ，、 and
    data class Success(
        val allItems: List<LibraryItem>,
        val filteredItems: List<LibraryItem>,
        val artistCount: Int,
        val playlistCount: Int,
        val albumCount: Int
    ) : LibraryUiState

    data class Error(val message: String) : LibraryUiState
}
