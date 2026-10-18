package com.lin0721.linmusic.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun LikedSongsScreen(
    onBack: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val likedSongs by viewModel.likedSongs.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val currentUid = viewModel.currentUid ?: userProfile?.uid?.toString()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LikedSongsContent(
            likedSongs = likedSongs,
            uid = currentUid,
            onPlaySongAt = { index -> viewModel.playLikedSongs(startIndex = index, shuffle = false) },
            onPlayAll = { viewModel.playLikedSongs(startIndex = 0, shuffle = false) },
            onShuffle = { viewModel.playLikedSongs(startIndex = 0, shuffle = true) },
            onToggleLike = { song -> viewModel.toggleLikeSong(song) },
            onSignInClick = onLoginClick,
            onBack = onBack
        )
    }
}
