package com.lin0721.linmusic.feature.recent.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.LocalBottomOverlayInset
import com.lin0721.linmusic.core.ui.components.EmptyState
import com.lin0721.linmusic.core.ui.components.ErrorState
import com.lin0721.linmusic.core.ui.components.SearchResultRowSkeleton
import com.lin0721.linmusic.core.ui.components.SecondaryScreenScaffold
import com.lin0721.linmusic.core.ui.components.SongRow
import com.lin0721.linmusic.core.ui.components.SongRowData
import com.lin0721.linmusic.core.ui.theme.BackgroundDark
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.feature.recent.domain.RecentSong
import com.lin0721.linmusic.feature.recent.domain.groupByPlayDay
import org.koin.androidx.compose.koinViewModel

private const val SKELETON_ROW_COUNT = 8

@Composable
fun RecentPlayScreen(
    viewModel: RecentPlayViewModel = koinViewModel(),
    onBack: () -> Unit,
    onPlaylistClick: (id: Long) -> Unit,
    onAlbumClick: (id: Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTrack by viewModel.playerManager.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()

    SecondaryScreenScaffold(title = "Recently Played", onBack = onBack) {
        when (val state = uiState) {
            RecentPlayUiState.Loading -> {
                Column(modifier = Modifier.padding(top = MimikyuSpacing.sm)) {
                    repeat(SKELETON_ROW_COUNT) { SearchResultRowSkeleton() }
                }
            }

            is RecentPlayUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorState(message = state.message, onRetry = { viewModel.load() })
                }
            }

            is RecentPlayUiState.Success -> {
                RecentPlayList(
                    state = state,
                    currentTrackId = currentTrack?.mediaId,
                    isPlaying = isPlaying,
                    onSongClick = { viewModel.playSong(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentPlayList(
    state: RecentPlayUiState.Success,
    currentTrackId: String?,
    isPlaying: Boolean,
    onSongClick: (RecentSong) -> Unit
) {
    if (state.songs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Rounded.History,
                title = "No listening history yet"
            )
        }
        return
    }

    // Group by day while preserving chronological descending order
    val songGroups = remember(state.songs) { state.songs.groupByPlayDay { it.playTime } }

    LazyColumn(
        contentPadding = PaddingValues(
            top = MimikyuSpacing.sm,
            bottom = LocalBottomOverlayInset.current + 16.dp
        )
    ) {
        songGroups.forEach { group ->
            stickyHeader(key = "header_${group.label}") { DayHeader(group.label) }
            itemsIndexed(group.items, key = { index, song -> "${group.label}_${song.id}_$index" }) { _, song ->
                val track = song.track
                SongRow(
                    data = SongRowData(
                        id = track.id,
                        title = song.title,
                        artist = song.artist,
                        coverUrl = song.coverUrl,
                        durationText = song.playedAtText.ifBlank { null }
                    ),
                    isActive = currentTrackId == track.id.toString(),
                    isPlaying = isPlaying,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun DayHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
