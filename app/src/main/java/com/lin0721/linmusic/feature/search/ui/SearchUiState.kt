package com.lin0721.linmusic.feature.search.ui

import com.lin0721.linmusic.feature.search.domain.HotSearch
import com.lin0721.linmusic.feature.search.domain.PlaylistTag
import com.lin0721.linmusic.feature.search.domain.SearchResultItem
import com.lin0721.linmusic.feature.search.domain.SearchSuggestion

// （//Curated Playlists）
sealed interface DiscoveryUiState {
    data object Loading : DiscoveryUiState
    data class Success(
        val defaultKeyword: String,
        val hotSearches: List<HotSearch>,
        val playlistTags: List<PlaylistTag>
    ) : DiscoveryUiState
    data class Error(val message: String) : DiscoveryUiState
}

// SearchType Tab  of  and
sealed interface SearchResultsUiState {
    data object Idle : SearchResultsUiState
    data object Loading : SearchResultsUiState
    data class Success(
        val items: List<SearchResultItem>,
        val totalCount: Int,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : SearchResultsUiState
    data object Empty : SearchResultsUiState
    data class Error(val message: String) : SearchResultsUiState
}

enum class SearchMode { Discovery, Typing, Results }

// suggestionQuery ，
data class SearchInputState(
    val query: String = "",
    val suggestions: List<SearchSuggestion> = emptyList(),
    val suggestionQuery: String = ""
) {
    val currentSuggestions: List<SearchSuggestion>
        get() = if (suggestionQuery == query) suggestions else emptyList()
}
