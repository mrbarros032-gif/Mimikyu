package com.lin0721.linmusic.feature.artist.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.core.ui.components.MimikyuTextButton
import com.lin0721.linmusic.core.ui.components.MimikyuButton
import com.lin0721.linmusic.core.ui.components.LoginBottomSheet
import com.lin0721.linmusic.core.ui.components.WebViewLoginScreen
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.ScreenSlideDurationMs
import org.koin.androidx.compose.koinViewModel

@Composable
fun ArtistScreen(
    artistId: Long,
    viewModel: ArtistViewModel = koinViewModel(),
    onBack: () -> Unit,
    onArtistClick: (Long) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onMvClick: (Long, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val likedSongIds by viewModel.likedSongIds.collectAsStateWithLifecycle()
    val collectState by viewModel.collectState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val currentTrack by viewModel.playerManager.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()

    var showLoginSheet by remember { mutableStateOf(false) }
    var showWebViewLogin by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collect { com.lin0721.linmusic.core.ui.components.ToastManager.showToast(it) }
    }
    LaunchedEffect(artistId) {
        viewModel.loadArtistData(artistId)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val state = uiState) {
            is ArtistUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is ArtistUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Failed to load", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(MimikyuSpacing.sm))
                        Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Spacer(Modifier.height(MimikyuSpacing.md))
                        MimikyuButton(
                            onClick = { viewModel.loadArtistData(artistId) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Retry", color = MaterialTheme.colorScheme.onPrimary)
                        }
                        MimikyuTextButton(onClick = onBack) { Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            is ArtistUiState.Success -> {
                val blockedArtistIds by viewModel.blockedArtistIds.collectAsStateWithLifecycle()
                ArtistContent(
                    artist = state.artist,
                    isFollowed = state.isFollowed,
                    fansCount = state.fansCount,
                    playCount = state.playCount,
                    mimikyuMonthlyListeners = state.mimikyuMonthlyListeners,
                    topSongs = state.topSongs,
                    albums = state.albums,
                    albumsHasMore = state.albumsHasMore,
                    albumsLoadingMore = state.albumsLoadingMore,
                    mvs = state.mvs,
                    mvsLoadingMore = state.mvsLoadingMore,
                    allSongs = state.allSongs,
                    allSongsLoadingMore = state.allSongsLoadingMore,
                    similarArtists = state.similarArtists,
                    likedSongIds = likedSongIds,
                    blockedArtistIds = blockedArtistIds,
                    currentTrackId = currentTrack?.mediaId,
                    isPlaying = isPlaying,
                    collectState = collectState,
                    isLoggedIn = userProfile != null,
                    onBack = onBack,
                    onArtistClick = onArtistClick,
                    onPlaylistClick = onPlaylistClick,
                    onAlbumClick = onAlbumClick,
                    onMvClick = onMvClick,
                    onFollowClick = { viewModel.toggleFollow(artistId) },
                    onBlockClick = { viewModel.toggleBlockArtist(artistId) },
                    onPlaySong = { track ->
                        viewModel.playSongInList(track, state.topSongs)
                    },
                    onPlayAll = {
                        state.topSongs.firstOrNull()?.let { first ->
                            viewModel.playSongInList(first, state.topSongs)
                        }
                    },
                    onLikeClick = { songId ->
                        viewModel.prepareCollectDialog(songId)
                    },
                    onToggleLike = { songId, like ->
                        viewModel.toggleLikeSong(songId, like)
                    },
                    onAddToPlayNext = { track ->
                        viewModel.addTrackToPlayNext(track)
                    },
                    onSaveCollection = { songId, items ->
                        viewModel.savePlaylistCollection(songId, items)
                    },
                    onSaveNewCollection = { name, songId ->
                        viewModel.createPlaylistAndAddSong(name, songId)
                    },
                    onRequireLogin = {
                        showLoginSheet = true
                    },
                    onLoadMoreAlbums = { viewModel.loadMoreAlbums() },
                    onLoadMoreMvs = { viewModel.loadMoreMvs() },
                    onLoadAllSongsIfNeeded = { viewModel.loadAllSongsIfNeeded() },
                    onLoadMoreAllSongs = { viewModel.loadMoreAllSongs() }
                )
            }
        }

        if (showLoginSheet) {
            LoginBottomSheet(
                onDismiss = { showLoginSheet = false },
                onWebLogin = {
                    showLoginSheet = false
                    showWebViewLogin = true
                },
                onLoginSuccess = { cookies ->
                    showLoginSheet = false
                    viewModel.handleLoginSuccess(cookies)
                }
            )
        }

        AnimatedVisibility(
            visible = showWebViewLogin,
            enter = slideInVertically(tween(ScreenSlideDurationMs)) { it } + fadeIn(tween(ScreenSlideDurationMs)),
            exit = slideOutVertically(tween(ScreenSlideDurationMs)) { it } + fadeOut(tween(ScreenSlideDurationMs))
        ) {
            WebViewLoginScreen(
                onClose = { showWebViewLogin = false },
                onLoginSuccess = { cookies ->
                    showWebViewLogin = false
                    // ， viewModel
                    viewModel.handleLoginSuccess(cookies)
                }
            )
        }
    }
}
