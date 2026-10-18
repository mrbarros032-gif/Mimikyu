package com.lin0721.linmusic.feature.home.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.LocalBottomOverlayInset
import com.lin0721.linmusic.feature.home.domain.HomeCard
import kotlinx.coroutines.flow.distinctUntilChanged

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.statusBarsPadding

private const val RECENT_PLAY_KEY = "recent_play"
private const val FOR_YOU_KEY = "for_you"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onPlaylistClick: (Long, Boolean) -> Unit,
    onArtistClick: (Long) -> Unit = {},
    onSongClick: (shelfTitle: String, List<HomeCard.Song>, HomeCard.Song) -> Unit,
    onVoiceClick: (shelfTitle: String, List<HomeCard.Voice>, HomeCard.Voice) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onIntelligenceClick: () -> Unit,
    onRoamingClick: () -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val feed = (uiState as? HomeUiState.Success)?.data

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LookaheadScope {
            CompositionLocalProvider(LocalHomeLookaheadScope provides this) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 100.dp,
                        bottom = LocalBottomOverlayInset.current + 16.dp
                    )
                ) {

                    when (uiState) {
                        is HomeUiState.Loading -> item { LoadingIndicator() }

                        is HomeUiState.Error -> item {
                            ErrorContent(message = uiState.message, onRetry = onRetry)
                        }

                        is HomeUiState.Success -> {
                            val data = uiState.data

                            // 1. Quick Access 2-Column Grid (6 items: Liked Songs, Daily Mixes, etc.)
                            if (data.recentPlaylists.isNotEmpty()) {
                                item(key = RECENT_PLAY_KEY) {
                                    RecentPlaySection(
                                        items = data.recentPlaylists,
                                        onClick = { item -> onPlaylistClick(item.id, false) },
                                        modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                                    )
                                }
                            }

                            // 2. Main Curated & Recommended Shelves (Curated YouTube Playlists & Curated Music)
                            items(
                                items = data.shelves,
                                key = { it.blockCode }
                            ) { shelf ->
                                HomeShelfSection(
                                    shelf = shelf,
                                    modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                                    onCardClick = { card ->
                                        when (card) {
                                            is HomeCard.Playlist -> onPlaylistClick(card.id, false)
                                            is HomeCard.Album -> onPlaylistClick(card.id, true)
                                            is HomeCard.Song -> onSongClick(shelf.title, shelf.cards.filterIsInstance<HomeCard.Song>(), card)
                                            is HomeCard.Voice -> onVoiceClick(shelf.title, shelf.cards.filterIsInstance<HomeCard.Voice>(), card)
                                        }
                                    }
                                )
                            }

                            // 3. "Your favourite artists" shelf (circular artist cards at the bottom)
                            if (data.favoriteArtists.isNotEmpty()) {
                                item(key = "favorite_artists_shelf") {
                                    MimikyuFavoriteArtistsShelf(
                                        items = data.favoriteArtists,
                                        onArtistClick = onArtistClick,
                                        modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                                    )
                                }
                            }


                            if (data.isLoadingMore) {
                                item { HomeShelfLoadingMore() }
                            }
                        }
                    }
                }
            }
        }
    }

    // LaunchedEffect  of  key， snapshotFlow  of ——
    // uiState  State，Create，。
    val canLoadMore = feed != null && feed.hasMore && !feed.isLoadingMore

    LaunchedEffect(listState, canLoadMore) {
        if (!canLoadMore) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1

            lastVisible >= info.totalItemsCount - 2
        }
            .distinctUntilChanged()
            .collect { if (it) onLoadMore() }
    }
}
