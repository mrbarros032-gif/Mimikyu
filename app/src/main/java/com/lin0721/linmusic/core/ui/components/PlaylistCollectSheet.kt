package com.lin0721.linmusic.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.ui.theme.RadiusCompact
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

// ────────────────────────────────────────────────────────────────────────────
// "Save to Playlist" ， playlist、artist 。
// ，。
// ────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistCollectSheet(
    songId: Long,
    collectState: PlaylistCollectState,
    onDismiss: () -> Unit,
    onSaveCollection: (Long, List<PlaylistCollectItem>) -> Unit,
    onSaveNewCollection: (String, Long) -> Unit,
    sheetShape: Shape = BottomSheetShape,
    itemCornerRadius: Dp = RadiusCompact,
    confirmButtonShape: Shape = RoundedCornerShape(10.dp)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = sheetShape,
        dragHandle = { MimikyuDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MimikyuSpacing.lg)
                .navigationBarsPadding()
                .padding(bottom = MimikyuSpacing.lg)
        ) {
            Text(
                text = "Save to Playlist",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = MimikyuSpacing.md)
            )

            if (collectState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val localItems = remember(collectState.songId) {
                    collectState.collectItems.map { it.copy() }.toMutableStateList()
                }
                // of Playlists ID，
                val userModifiedIds = remember(collectState.songId) { mutableSetOf<Long>() }

                LaunchedEffect(collectState.collectItems) {
                    collectState.collectItems.forEach { remoteItem ->
                        val localIndex = localItems.indexOfFirst { it.playlistId == remoteItem.playlistId }
                        if (localIndex != -1) {
                            val local = localItems[localIndex]
                            if (!userModifiedIds.contains(remoteItem.playlistId)) {
                                if (local.isContains != remoteItem.isContains || local.isInitiallyContains != remoteItem.isInitiallyContains) {
                                    localItems[localIndex] = local.copy(
                                        isInitiallyContains = remoteItem.isInitiallyContains,
                                        isContains = remoteItem.isContains
                                    )
                                }
                            }
                        } else {
                            localItems.add(remoteItem.copy())
                        }
                    }
                }

                var showCreatePlaylistDialog by remember { mutableStateOf(false) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(MimikyuSpacing.sm)
                ) {
                    item(key = "create_new_playlist_item") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCreatePlaylistDialog = true
                                }
                                .padding(vertical = MimikyuSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(itemCornerRadius))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Playlist",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "New Playlist",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (localItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = MimikyuSpacing.lg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No other playlists available", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                    } else {
                        itemsIndexed(localItems, key = { _, item -> item.playlistId }) { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        userModifiedIds.add(item.playlistId)
                                        localItems[index] = item.copy(isContains = !item.isContains)
                                    }
                                    .padding(vertical = MimikyuSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    SubcomposeAsyncImage(
                                        model = "${item.coverUrl}?param=100y100",
                                        contentDescription = item.playlistName,
                                        contentScale = ContentScale.Crop,
                                        loading = { CoverPlaceholder() },
                                        error = { CoverPlaceholder() },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(itemCornerRadius))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = item.playlistName,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Checkbox(
                                    checked = item.isContains,
                                    onCheckedChange = { checked ->
                                        userModifiedIds.add(item.playlistId)
                                        localItems[index] = item.copy(isContains = checked)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MimikyuSpacing.lg))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    MimikyuTextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(MimikyuSpacing.sm))
                    MimikyuButton(
                        onClick = {
                            onSaveCollection(songId, localItems)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = confirmButtonShape
                    ) {
                        Text("Confirm", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                if (showCreatePlaylistDialog) {
                    CreatePlaylistDialog(
                        onDismiss = { showCreatePlaylistDialog = false },
                        onCreate = { name -> onSaveNewCollection(name, songId) }
                    )
                }
            }
        }
    }
}
