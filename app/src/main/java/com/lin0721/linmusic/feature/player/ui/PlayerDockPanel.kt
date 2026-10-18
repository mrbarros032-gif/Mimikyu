package com.lin0721.linmusic.feature.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.media3.common.MediaItem
import com.lin0721.linmusic.core.ui.theme.LocalMimikyuSystemBarsConsumed
import com.lin0721.linmusic.core.ui.theme.rememberMimikyuPlayerPanelWidth

// Expanded  of PlayExpand： of  and ，
// of （ MimikyuApp.kt）。 FullPlayerScreen，onClose/onDragClose
// CollapsePlay，"Close player and restart"。
// and ，
@Composable
fun PlayerDockPanel(
    currentTrack: MediaItem?,
    isPlaying: Boolean,
    currentPositionProvider: () -> Long,
    duration: Long,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit,
    onDragClose: (Float, Float) -> Unit,
    onArtistClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onNavigateToProfile: (Long) -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    // → of （0~1） and  of
    fullscreenProgress: () -> Float,
    fullscreenWidth: Dp
) {
    if (currentTrack == null) return

    val panelWidth = rememberMimikyuPlayerPanelWidth()

    // （）；， of  FullPlayerScreen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(WindowInsets.statusBars.union(WindowInsets.navigationBars))
    ) {
        CompositionLocalProvider(LocalMimikyuSystemBarsConsumed provides true) {
            FullPlayerScreen(
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                currentPositionProvider = currentPositionProvider,
                duration = duration,
                onTogglePlay = onTogglePlay,
                onSeek = onSeek,
                onClose = onClose,
                isPlayerOpen = true,
                onArtistClick = onArtistClick,
                onAlbumClick = onAlbumClick,
                onNavigateToProfile = onNavigateToProfile,
                onDragClose = onDragClose,
                fitCoverToViewport = true,
                contentMaxWidth = panelWidth,
                onToggleSidebarFullscreen = onToggleFullscreen,
                isSidebarFullscreen = isFullscreen,
                sidebarFullscreenProgress = fullscreenProgress,
                fullscreenContentWidth = fullscreenWidth
            )
        }
    }
}
