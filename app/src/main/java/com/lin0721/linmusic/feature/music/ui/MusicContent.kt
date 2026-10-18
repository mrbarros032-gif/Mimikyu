package com.lin0721.linmusic.feature.music.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.LocalBottomOverlayInset
import com.lin0721.linmusic.feature.home.ui.ErrorContent
import com.lin0721.linmusic.feature.home.ui.LoadingIndicator
import com.lin0721.linmusic.feature.music.domain.StyleArtistItem
import com.lin0721.linmusic.feature.music.domain.StylePlaylistItem

// 「」tab ： HomeScreen ， of Genres。
// and 「All」 Content， tab 。
@Composable
fun MusicContent(
    uiState: MusicUiState,
    onStyleSelect: (StyleSelection) -> Unit,
    onChildStyleSelect: (Long?) -> Unit,
    onPlaylistClick: (StylePlaylistItem) -> Unit,
    onArtistClick: (StyleArtistItem) -> Unit,
    onPlaySongAt: (Int) -> Unit,
    onPlayFavourite: () -> Unit,
    onRetry: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalBottomOverlayInset.current + 16.dp)
    ) {
        when (uiState) {
            is MusicUiState.Loading -> item { LoadingIndicator() }

            is MusicUiState.Error -> item {
                ErrorContent(message = uiState.message, onRetry = onRetry)
            }

            is MusicUiState.Success -> {
                val data = uiState.data

                item {
                    MusicStyleChips(
                        styles = data.styles,
                        selection = data.selection,
                        showPreference = data.hasPreference,
                        onSelect = onStyleSelect
                    )
                }

                // and ，Genres and
                if (data.selection is StyleSelection.Preference) {
                    data.content?.head?.portrait?.let { portrait ->
                        item {
                            MusicPortraitCard(
                                portrait = portrait,
                                accent = data.preferences.firstOrNull()?.colorHex.toStyleColor()
                            )
                        }
                    }
                    item { MusicSectionTitle("Preferences") }
                    item { MusicPreferenceBars(data.preferences) }
                } else {
                    data.content?.head?.let { head ->
                        item { MusicStyleHeader(head = head, onPlay = { onPlaySongAt(0) }) }
                    }
                    data.selectedStyle?.children?.takeIf { it.isNotEmpty() }?.let { children ->
                        item {
                            MusicSubStyleChips(
                                children = children,
                                selectedChildId = data.selectedChildId,
                                onSelect = onChildStyleSelect
                            )
                        }
                    }
                }

                if (data.isContentLoading) {
                    item { MusicSectionLoading() }
                } else {
                    val content = data.content

                    content?.head?.favouriteSong?.let { track ->
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                MusicSectionTitle("Your Favorites")
                                MusicFavouriteSongCard(track = track, onClick = onPlayFavourite)
                            }
                        }
                    }

                    content?.playlists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                        item { MusicSectionTitle("Popular Playlists") }
                        item { MusicPlaylistRow(playlists = playlists, onClick = onPlaylistClick) }
                    }

                    content?.songs?.takeIf { it.isNotEmpty() }?.let { songs ->
                        item { MusicSectionTitle("Essential Tracks") }
                        item { MusicSongList(songs = songs, onPlayAt = onPlaySongAt) }
                    }

                    content?.artists?.takeIf { it.isNotEmpty() }?.let { artists ->
                        item { MusicSectionTitle("Top Artists") }
                        item { MusicArtistRow(artists = artists, onClick = onArtistClick) }
                    }
                }
            }
        }
    }
}
