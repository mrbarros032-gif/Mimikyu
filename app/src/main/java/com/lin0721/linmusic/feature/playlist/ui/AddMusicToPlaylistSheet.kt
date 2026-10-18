package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.ui.components.CoverPlaceholder
import com.lin0721.linmusic.core.ui.components.MimikyuDragHandle
import com.lin0721.linmusic.core.ui.components.MimikyuIconButton
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.RadiusCompact

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.lin0721.linmusic.core.localmusic.LocalTrack
import com.lin0721.linmusic.core.localmusic.LocalMusicRepository
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import androidx.compose.runtime.LaunchedEffect

enum class AddMusicSource { YOUTUBE, LOCAL }

// SearchAdd
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMusicToPlaylistSheet(
    searchQuery: String,
    searchState: AddMusicSearchState,
    existingTrackIds: Set<Long>,
    // PlaylistsDetails"RecommendedSongs" of ，Playlists；
    // ， and Search， of ，
    recommendedSongs: List<Track>,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onAddTrack: (Track, onComplete: () -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val focusManager = LocalFocusManager.current
    var addingTrackIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedSource by remember { mutableStateOf(AddMusicSource.YOUTUBE) }

    val localMusicRepo = remember {
        org.koin.core.context.GlobalContext.get().getOrNull<LocalMusicRepository>()
    }
    var localTracks by remember { mutableStateOf<List<LocalTrack>>(emptyList()) }
    var isLoadingLocal by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSource) {
        if (selectedSource == AddMusicSource.LOCAL && localTracks.isEmpty()) {
            isLoadingLocal = true
            localTracks = localMusicRepo?.scan().orEmpty()
            isLoadingLocal = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = BottomSheetShape,
        dragHandle = { MimikyuDragHandle() },
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Header and Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MimikyuSpacing.md)
                    .padding(bottom = MimikyuSpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Songs to Playlist",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                MimikyuIconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Source Selector Tabs (YouTube Music & Device Library)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MimikyuSpacing.md)
                    .padding(bottom = MimikyuSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSource == AddMusicSource.YOUTUBE,
                    onClick = { selectedSource = AddMusicSource.YOUTUBE },
                    label = { Text("YouTube Music", fontSize = 13.sp) },
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = selectedSource == AddMusicSource.LOCAL,
                    onClick = { selectedSource = AddMusicSource.LOCAL },
                    label = { Text("Device Library", fontSize = 13.sp) },
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MimikyuSpacing.md)
                    .padding(bottom = MimikyuSpacing.sm)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    if (selectedSource == AddMusicSource.YOUTUBE) "Search songs on YouTube" else "Search songs in device library",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = onClearQuery,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (selectedSource == AddMusicSource.LOCAL) {
                val filteredLocal = remember(localTracks, searchQuery) {
                    if (searchQuery.isBlank()) localTracks
                    else localTracks.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true) ||
                        (it.album?.contains(searchQuery, ignoreCase = true) == true)
                    }
                }

                if (isLoadingLocal) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                    }
                } else if (filteredLocal.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "No audio files found on device" else "No matching local songs",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = MimikyuSpacing.md)
                    ) {
                        item(key = "local_header") {
                            Text(
                                text = "Device Audio Files (${filteredLocal.size})",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    horizontal = MimikyuSpacing.md,
                                    vertical = MimikyuSpacing.sm
                                )
                            )
                        }
                        items(items = filteredLocal, key = { "local_${it.mediaStoreId}_${it.songId ?: 0}" }) { localItem ->
                            val track = remember(localItem) { localItem.toPlaylistTrack() }
                            val isAdded = existingTrackIds.contains(track.id)
                            val isAdding = addingTrackIds.contains(track.id)

                            AddTrackRow(
                                track = track,
                                isAdded = isAdded,
                                isAdding = isAdding,
                                onAdd = {
                                    addingTrackIds = addingTrackIds + track.id
                                    onAddTrack(track) {
                                        addingTrackIds = addingTrackIds - track.id
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // YouTube Search Results
                when (searchState) {
                    is AddMusicSearchState.Idle -> {
                        if (recommendedSongs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Type keywords to search songs",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(bottom = MimikyuSpacing.md)
                            ) {
                                item(key = "recommend_header") {
                                    Text(
                                        text = "Recommended Songs",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(
                                            horizontal = MimikyuSpacing.md,
                                            vertical = MimikyuSpacing.sm
                                        )
                                    )
                                }
                                items(items = recommendedSongs, key = { "rec_${it.id}" }) { track ->
                                    val isAdded = existingTrackIds.contains(track.id)
                                    val isAdding = addingTrackIds.contains(track.id)

                                    AddTrackRow(
                                        track = track,
                                        isAdded = isAdded,
                                        isAdding = isAdding,
                                        onAdd = {
                                            addingTrackIds = addingTrackIds + track.id
                                            onAddTrack(track) {
                                                addingTrackIds = addingTrackIds - track.id
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    is AddMusicSearchState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }
                    is AddMusicSearchState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = MimikyuSpacing.md),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = searchState.message,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
                            )
                        }
                    }
                    is AddMusicSearchState.Success -> {
                        if (searchState.tracks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No songs found",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(bottom = MimikyuSpacing.md)
                            ) {
                                items(items = searchState.tracks, key = { it.id }) { track ->
                                    val isAdded = existingTrackIds.contains(track.id)
                                    val isAdding = addingTrackIds.contains(track.id)

                                    AddTrackRow(
                                        track = track,
                                        isAdded = isAdded,
                                        isAdding = isAdding,
                                        onAdd = {
                                            addingTrackIds = addingTrackIds + track.id
                                            onAddTrack(track) {
                                                addingTrackIds = addingTrackIds - track.id
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTrackRow(
    track: Track,
    isAdded: Boolean,
    isAdding: Boolean,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MimikyuSpacing.md, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        val imageRequest = remember(track.al.picUrl) {
            if (track.al.picUrl.isNotBlank()) {
                ImageRequest.Builder(context)
                    .data("${track.al.picUrl}?param=100y100")
                    .crossfade(true)
                    .build()
            } else null
        }

        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = track.name,
            contentScale = ContentScale.Crop,
            loading = { CoverPlaceholder() },
            error = { CoverPlaceholder() },
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(RadiusCompact))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val artistAndAlbum = buildString {
                append(track.ar.joinToString("/") { it.name })
                if (track.al.name.isNotBlank()) {
                    append(" - ")
                    append(track.al.name)
                }
            }
            Text(
                text = artistAndAlbum,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        when {
            isAdding -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(36.dp)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
            isAdded -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Added",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(36.dp)
                        .padding(6.dp)
                )
            }
            else -> {
                MimikyuIconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private fun LocalTrack.toPlaylistTrack(): Track {
    val tid = songId?.takeIf { it > 0 } ?: (mediaStoreId.takeIf { it > 0 } ?: (title.hashCode().toLong() and 0x7FFFFFFF))
    val placeholder = YouTubeIdRegistry.registerPlaceholder(
        title = title,
        artist = artist,
        album = album.orEmpty()
    )
    val converted = placeholder.copy(
        id = tid,
        name = title,
        fee = 0
    )
    YouTubeIdRegistry.registerTrack(converted)
    return converted
}

