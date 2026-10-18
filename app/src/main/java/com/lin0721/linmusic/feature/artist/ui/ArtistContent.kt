package com.lin0721.linmusic.feature.artist.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.LocalBottomOverlayInset
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.ui.components.PlaylistCollectItem
import com.lin0721.linmusic.core.ui.components.PlaylistCollectSheet
import com.lin0721.linmusic.core.ui.components.PlaylistCollectState
import com.lin0721.linmusic.core.model.ArtistAlbum
import com.lin0721.linmusic.core.model.ArtistDetailInfo
import com.lin0721.linmusic.core.model.ArtistInfo
import com.lin0721.linmusic.core.model.ArtistMv
import com.lin0721.linmusic.core.ui.theme.CoverPlaceholderDark
import com.lin0721.linmusic.feature.playlist.ui.PlaylistSongOptionsSheet

// ArtistsDetails： and ， Tab
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistContent(
    artist: ArtistDetailInfo,
    isFollowed: Boolean,
    fansCount: Long,
    playCount: Long = 0L,
    mimikyuMonthlyListeners: Long = 0L,
    topSongs: List<Track>,
    albums: List<ArtistAlbum>,
    albumsHasMore: Boolean,
    albumsLoadingMore: Boolean,
    mvs: List<ArtistMv>,
    mvsLoadingMore: Boolean,
    allSongs: List<Track>,
    allSongsLoadingMore: Boolean,
    similarArtists: List<ArtistInfo>,
    likedSongIds: Set<Long>,
    blockedArtistIds: Set<Long>,
    currentTrackId: String?,
    isPlaying: Boolean,
    collectState: PlaylistCollectState,
    isLoggedIn: Boolean,
    onBack: () -> Unit,
    onArtistClick: (Long) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onMvClick: (Long, String) -> Unit,
    onFollowClick: () -> Unit,
    onBlockClick: () -> Unit,
    onPlaySong: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onLikeClick: (Long) -> Unit,
    onToggleLike: (Long, Boolean) -> Unit,
    onAddToPlayNext: (Track) -> Unit,
    onSaveCollection: (Long, List<PlaylistCollectItem>) -> Unit,
    onSaveNewCollection: (String, Long) -> Unit,
    onRequireLogin: () -> Unit,
    onLoadMoreAlbums: () -> Unit,
    onLoadMoreMvs: () -> Unit,
    onLoadAllSongsIfNeeded: () -> Unit,
    onLoadMoreAllSongs: () -> Unit
) {
    val density = LocalDensity.current
    val listState = rememberLazyListState()

    var showMoreMenuSheet by remember { mutableStateOf(false) }

    var dominantColor by remember { mutableStateOf(CoverPlaceholderDark) }


    val collapseThresholdPx = with(density) { 320.dp.toPx() }
    val progress by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / collapseThresholdPx).coerceIn(0f, 1f)
        }
    }

    // + ： and  ArtistBackdrop  of +
    val headerHeightPx = with(density) { 310.dp.toPx() }
    val actionBarHeightPx = with(density) { 80.dp.toPx() }
    val backdropCoverThresholdPx = headerHeightPx + actionBarHeightPx
    // of ：， of ，Songs
    val backdropCoveredPx by remember {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset.toFloat()
            when {
                index <= 0 -> offset
                index == 1 -> headerHeightPx + offset
                else -> backdropCoverThresholdPx
            }.coerceIn(0f, backdropCoverThresholdPx)
        }
    }

    var collectSongId by remember { mutableStateOf<Long?>(null) }
    var showBioDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var musicSubTab by remember { mutableStateOf(0) }
    var optionsTrack by remember { mutableStateOf<Track?>(null) }

    // 「AllSongs」 Tab
    LaunchedEffect(musicSubTab) {
        if (musicSubTab == 1) onLoadAllSongsIfNeeded()
    }

    // ，Current Tab  of
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 5
        }
    }
    LaunchedEffect(shouldLoadMore, selectedTab, musicSubTab) {
        if (!shouldLoadMore) return@LaunchedEffect
        when (selectedTab) {
            0 -> if (musicSubTab == 1) onLoadMoreAllSongs()
            1 -> onLoadMoreAlbums()
            2 -> onLoadMoreMvs()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        ArtistBackdrop(
            artist = artist,
            dominantColor = dominantColor,
            onDominantColorChange = { dominantColor = it }
        )

        // ：，，Songs
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = backdropCoverThresholdPx - backdropCoveredPx }
                .background(MaterialTheme.colorScheme.background)
        )


        @OptIn(ExperimentalFoundationApi::class)
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = LocalBottomOverlayInset.current + 16.dp)
            ) {
                item(key = "header_placeholder") {
                    ArtistNameSection(artist = artist, progress = progress, tintColor = dominantColor)
                }

                item(key = "action_bar") {
                    ArtistActionBar(
                        artist = artist,
                        fansCount = fansCount,
                        mimikyuMonthlyListeners = mimikyuMonthlyListeners,
                        isFollowed = isFollowed,
                        onFollowClick = onFollowClick,
                        onMoreClick = { showMoreMenuSheet = true },
                        onPlayAll = onPlayAll
                    )
                }

                item(key = "tab_bar") {
                    ArtistTabBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }

                // of  Tab
                when (selectedTab) {
                    0 -> artistMusicTab(
                        hotSongs = topSongs,
                        allSongs = allSongs,
                        musicSubTab = musicSubTab,
                        onMusicSubTabSelected = { musicSubTab = it },
                        allSongsLoadingMore = allSongsLoadingMore,
                        likedSongIds = likedSongIds,
                        currentTrackId = currentTrackId,
                        isPlaying = isPlaying,
                        isLoggedIn = isLoggedIn,
                        onPlaySong = onPlaySong,
                        onLikeClick = onLikeClick,
                        onOpenCollectSheet = { collectSongId = it },
                        onOpenMoreOptions = { track ->
                            if (!isLoggedIn) onRequireLogin() else optionsTrack = track
                        },
                        onRequireLogin = onRequireLogin
                    )
                    1 -> artistAlbumTab(
                        albums = albums,
                        loadingMore = albumsLoadingMore,
                        onAlbumClick = onAlbumClick
                    )
                    2 -> artistAboutTab(
                        artist = artist,
                        fansCount = fansCount,
                        playCount = playCount,
                        mimikyuMonthlyListeners = mimikyuMonthlyListeners,
                        similarArtists = similarArtists,
                        onShowBioDialog = { showBioDialog = true },
                        onArtistClick = onArtistClick
                    )
                }
            }
        }

        ArtistTopBarOverlay(
            artistName = artist.name,
            progress = progress,
            dominantColor = dominantColor,
            onBack = onBack
        )

        // Details Bottom Sheet (Save)
        if (collectSongId != null) {
            PlaylistCollectSheet(
                songId = collectSongId!!,
                collectState = collectState,
                onDismiss = { collectSongId = null },
                onSaveCollection = onSaveCollection,
                onSaveNewCollection = onSaveNewCollection,
                confirmButtonShape = RoundedCornerShape(12.dp)
            )
        }

        // Songs「More Options」Bottom Sheet（NextPlay/Like/Save to Playlist/Albums；ArtistsDetails「Artists」）
        optionsTrack?.let { track ->
            PlaylistSongOptionsSheet(
                track = track,
                isLiked = track.id in likedSongIds,
                isLoggedIn = isLoggedIn,
                onDismiss = { optionsTrack = null },
                onAddToPlayNext = onAddToPlayNext,
                onToggleLike = onToggleLike,
                onCollectClick = { songId ->
                    collectSongId = songId
                    onLikeClick(songId)
                },
                onArtistClick = onArtistClick,
                onAlbumClick = onAlbumClick,
                onRequireLogin = onRequireLogin,
                showArtistOption = false
            )
        }

        // Artists Dialog
        if (showBioDialog) {
            ArtistBioDialog(
                artist = artist,
                onDismiss = { showBioDialog = false }
            )
        }

        // More Options Bottom Sheet
        if (showMoreMenuSheet) {
            ArtistMoreMenuSheet(
                isBlocked = blockedArtistIds.contains(artist.id),
                onBlockClick = onBlockClick,
                onDismiss = { showMoreMenuSheet = false }
            )
        }
    }
}
