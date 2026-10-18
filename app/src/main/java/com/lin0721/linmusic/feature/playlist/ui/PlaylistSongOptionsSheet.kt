package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import com.lin0721.linmusic.core.ui.theme.MimikyuIcons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.lin0721.linmusic.core.ui.components.CoverPlaceholder
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.ui.components.MimikyuDragHandle
import com.lin0721.linmusic.core.ui.components.ToastManager
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

// ────────────────────────────────────────────────────────────────────────────
// Songs"More Options"（NextPlay/Like/Save to Playlist/Artists/Albums）
// ： and  feature/player/ui/SongMoreOptionsSheet.kt  of
// （/，Play），。
// ────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSongOptionsSheet(
    track: Track,
    isLiked: Boolean,
    isLoggedIn: Boolean,
    onDismiss: () -> Unit,
    onAddToPlayNext: (Track) -> Unit,
    onToggleLike: (Long, Boolean) -> Unit,
    onCollectClick: (Long) -> Unit,
    onArtistClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onRequireLogin: () -> Unit,
    // ArtistsDetails「Artists」Current， false
    showArtistOption: Boolean = true,
    // CurrentPlaylistsCreatePlaylistsSongs， and Create
    canRemoveFromPlaylist: Boolean = false,
    onRemoveFromPlaylist: (Long) -> Unit = {},
    onDownloadClick: ((Track) -> Unit)? = null,

    extraOptions: @Composable ColumnScope.() -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        shape = BottomSheetShape,
        dragHandle = { MimikyuDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = MimikyuSpacing.md)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val coverModel = remember(track.al.picUrl) {
                    val url = track.al.picUrl
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        "$url?param=150y150"
                    } else {
                        url
                    }
                }
                SubcomposeAsyncImage(
                    model = coverModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    loading = { CoverPlaceholder() },
                    error = { CoverPlaceholder() },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(MaterialTheme.shapes.small)
                )
                Spacer(modifier = Modifier.width(MimikyuSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(MimikyuSpacing.xs))
                    Text(
                        text = track.ar.joinToString(" • ") { it.name },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = MimikyuSpacing.sm)
            )

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OptionRow(
                    iconRes = MimikyuIcons.AddToQueue,
                    text = "Play Next",
                    onClick = {
                        onDismiss()
                        onAddToPlayNext(track)
                    }
                )

                val likeIconRes = if (isLiked) MimikyuIcons.HeartFilled else MimikyuIcons.Heart
                val likeIconTint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                val likeText = if (isLiked) "Unlike" else "Like"

                OptionRow(
                    iconRes = likeIconRes,
                    text = likeText,
                    iconTint = likeIconTint,
                    onClick = {
                        onDismiss()
                        if (!isLoggedIn) {
                            onRequireLogin()
                        } else {
                            onToggleLike(track.id, !isLiked)
                        }
                    }
                )

                OptionRow(
                    iconRes = MimikyuIcons.AddToPlaylist,
                    text = "Add to Playlist",
                    onClick = {
                        onDismiss()
                        onCollectClick(track.id)
                    }
                )

                if (onDownloadClick != null) {
                    OptionRow(
                        iconRes = MimikyuIcons.Download,
                        text = "Download",
                        onClick = {
                            onDismiss()
                            onDownloadClick(track)
                        }
                    )
                }

                if (showArtistOption) {
                    val artistsText = track.ar.joinToString(" • ") { it.name }
                    OptionRow(
                        iconRes = MimikyuIcons.User,
                        text = "Artist: $artistsText",
                        onClick = {
                            onDismiss()
                            val artistId = track.ar.firstOrNull()?.id ?: 0L
                            if (artistId > 0) {
                                onArtistClick(artistId)
                            } else {
                                ToastManager.showToast("No artist info")
                            }
                        }
                    )
                }

                if (canRemoveFromPlaylist) {
                    OptionRow(
                        iconRes = MimikyuIcons.Close,
                        text = "Remove from Playlist",
                        onClick = {
                            onDismiss()
                            onRemoveFromPlaylist(track.id)
                        }
                    )
                }

                OptionRow(
                    iconRes = MimikyuIcons.Album,
                    text = "Album: ${track.al.name}",
                    onClick = {
                        onDismiss()
                        if (track.al.id > 0) {
                            onAlbumClick(track.al.id)
                        } else {
                            ToastManager.showToast("No album info")
                        }
                    }
                )

                extraOptions()
            }
        }
    }
}
