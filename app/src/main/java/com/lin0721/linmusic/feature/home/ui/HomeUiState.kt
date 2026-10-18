package com.lin0721.linmusic.feature.home.ui

import com.lin0721.linmusic.core.player.recommendation.DailyMixPlaylist
import com.lin0721.linmusic.feature.home.data.PersonalizedPlaylist
import com.lin0721.linmusic.feature.home.data.DailySong
import com.lin0721.linmusic.feature.recent.domain.RecentPlaylist
import com.lin0721.linmusic.feature.home.domain.HomeShelf
import com.lin0721.linmusic.feature.home.domain.ToplistInfo

// UI
sealed interface HomeUiState {
    data object Loading : HomeUiState

    // Success state
    data class Success(val data: HomeFeedData) : HomeUiState

    data class Error(val message: String) : HomeUiState
}

// Home feed data model
data class HomeFeedData(
    val shelves: List<HomeShelf> = emptyList(),
    val recommendPlaylists: List<PersonalizedPlaylist> = emptyList(),
    val recentPlaylists: List<RecentPlaylist> = emptyList(),
    val dailySongs: List<DailySong> = emptyList(),
    val toplistItems: List<ToplistInfo> = emptyList(),
    /** Generated Daily Mix playlists from DailyMixGenerator */
    val dailyMixes: List<DailyMixPlaylist> = emptyList(),
    /** 7-10 Top Mixes (Daily Mixes + Vibe Mixes: Pop, Chill, Happy, R&B, etc.) */
    val topMixes: List<DailyMixPlaylist> = emptyList(),
    /** 7-10 Recommended Stations (Artist Radios with Last.fm similarity) */
    val recommendedStations: List<DailyMixPlaylist> = emptyList(),
    /** 7-10 "THIS IS [Artist]" essential spotlight playlists */
    val bestOfArtists: List<DailyMixPlaylist> = emptyList(),
    /** 7-10 Favorite artists circular cards */
    val favoriteArtists: List<DailyMixPlaylist> = emptyList(),
    /** "More like [Artist]" shelf with 7-10 items */
    val moreLikeMixes: List<DailyMixPlaylist> = emptyList(),
    val moreLikeArtistName: String = "",
    val moreLikeArtistCover: String = "",
    /** "Made for your vibe" personal shelves with artist cutouts */
    val madeForYourVibe: List<com.lin0721.linmusic.core.ui.components.ThisIsCoverSpec> = emptyList(),
    // Pagination
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false
)
