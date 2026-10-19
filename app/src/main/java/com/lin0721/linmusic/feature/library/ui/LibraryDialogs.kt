package com.lin0721.linmusic.feature.library.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lin0721.linmusic.core.localmusic.LocalMusicRepository
import com.lin0721.linmusic.core.localmusic.LocalTrack
import com.lin0721.linmusic.core.model.Album
import com.lin0721.linmusic.core.model.Artist
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import com.lin0721.linmusic.core.ui.components.MimikyuButton
import com.lin0721.linmusic.core.ui.components.MimikyuDragHandle
import com.lin0721.linmusic.core.ui.components.MimikyuTextButton
import com.lin0721.linmusic.core.ui.components.ToastManager
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.ui.theme.MimikyuIcons
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

// ────────────────────────────────────────────────────────────────────────────
// "New Playlist" with YouTube Music search & Local Library selector
// ────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryCreatePlaylistSheet(
    onDismiss: () -> Unit,
    onCreate: (String, List<Track>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var playlistNameInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = YouTube Music, 1 = Local Library
    val selectedTracks = remember { mutableStateListOf<Track>() }

    val localMusicRepository: LocalMusicRepository = koinInject()
    var localTracks by remember { mutableStateOf<List<LocalTrack>>(emptyList()) }
    var localSearchQuery by remember { mutableStateOf("") }
    var isLoadingLocal by remember { mutableStateOf(false) }

    var ytQuery by remember { mutableStateOf("") }
    var ytResults by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var isSearchingYt by remember { mutableStateOf(false) }
    var ytSearchJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoadingLocal = true
        localTracks = runCatching { localMusicRepository.scan() }.getOrDefault(emptyList())
        isLoadingLocal = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = BottomSheetShape,
        dragHandle = { MimikyuDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding()
                .padding(horizontal = MimikyuSpacing.lg)
        ) {
            Text(
                text = "New Playlist",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = MimikyuSpacing.sm)
            )

            // Playlist Name Input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = MimikyuSpacing.md),
                contentAlignment = Alignment.CenterStart
            ) {
                if (playlistNameInput.isEmpty()) {
                    Text("Enter playlist name...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                BasicTextField(
                    value = playlistNameInput,
                    onValueChange = { playlistNameInput = it },
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(MimikyuSpacing.md))

            // Source Selector Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("YouTube Music", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Local Library (${localTracks.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            Spacer(modifier = Modifier.height(MimikyuSpacing.sm))

            if (selectedTab == 0) {
                // YouTube Music Search Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = MimikyuSpacing.sm),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(MimikyuSpacing.xs))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (ytQuery.isEmpty()) {
                                Text("Search YouTube Music songs...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                            BasicTextField(
                                value = ytQuery,
                                onValueChange = { query ->
                                    ytQuery = query
                                    ytSearchJob?.cancel()
                                    if (query.trim().length >= 2) {
                                        ytSearchJob = scope.launch {
                                            delay(350)
                                            isSearchingYt = true
                                            val res = withContext(Dispatchers.IO) {
                                                YouTube.search(query.trim(), YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                            }
                                            ytResults = res?.items?.filterIsInstance<SongItem>().orEmpty()
                                            isSearchingYt = false
                                        }
                                    } else {
                                        ytResults = emptyList()
                                        isSearchingYt = false
                                    }
                                },
                                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        if (ytQuery.isNotEmpty()) {
                            IconButton(onClick = { ytQuery = ""; ytResults = emptyList() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MimikyuSpacing.xs))

                if (isSearchingYt) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                    }
                } else if (ytResults.isEmpty() && ytQuery.length >= 2) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No songs found on YouTube Music", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(ytResults, key = { it.id }) { songItem ->
                            val isSelected = selectedTracks.any { it.name == songItem.title && it.ar.firstOrNull()?.name == songItem.artists.firstOrNull()?.name }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val songId = YouTubeIdRegistry.register(
                                            videoId = songItem.id,
                                            title = songItem.title,
                                            artist = songItem.artists.joinToString(", ") { it.name },
                                            coverUrl = songItem.thumbnail.orEmpty(),
                                            durationSec = songItem.duration ?: 0
                                        )
                                        val track = Track(
                                            id = songId,
                                            name = songItem.title,
                                            ar = songItem.artists.map { Artist(name = it.name) },
                                            al = Album(name = songItem.album?.name.orEmpty(), picUrl = songItem.thumbnail),
                                            dt = (songItem.duration?.toLong() ?: 0L) * 1000L
                                        )
                                        if (isSelected) {
                                            selectedTracks.removeAll { it.name == songItem.title && it.ar.firstOrNull()?.name == songItem.artists.firstOrNull()?.name }
                                        } else {
                                            selectedTracks.add(track)
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = songItem.thumbnail,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(MimikyuSpacing.sm))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = songItem.title,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = songItem.artists.joinToString(", ") { it.name },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                    } else {
                                        Icon(painter = painterResource(MimikyuIcons.Plus), contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Local Library Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = MimikyuSpacing.sm),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(MimikyuSpacing.xs))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (localSearchQuery.isEmpty()) {
                                Text("Filter local songs...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                            BasicTextField(
                                value = localSearchQuery,
                                onValueChange = { localSearchQuery = it },
                                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        if (localSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { localSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MimikyuSpacing.xs))

                val filteredLocalTracks = remember(localTracks, localSearchQuery) {
                    if (localSearchQuery.isBlank()) localTracks
                    else localTracks.filter {
                        it.title.contains(localSearchQuery, ignoreCase = true) ||
                                it.artist.contains(localSearchQuery, ignoreCase = true)
                    }
                }

                if (filteredLocalTracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (localTracks.isEmpty()) "No local music found on device" else "No matching local songs",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredLocalTracks, key = { it.uri.toString() }) { localTrack ->
                            val isSelected = selectedTracks.any { it.localUri == localTrack.uri.toString() }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val track = Track(
                                            id = -localTrack.mediaStoreId,
                                            name = localTrack.title,
                                            ar = listOf(Artist(name = localTrack.artist)),
                                            al = Album(name = localTrack.album.orEmpty()),
                                            dt = localTrack.durationMs,
                                            localUri = localTrack.uri.toString()
                                        )
                                        if (isSelected) {
                                            selectedTracks.removeAll { it.localUri == localTrack.uri.toString() }
                                        } else {
                                            selectedTracks.add(track)
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(MimikyuIcons.Track),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp).padding(4.dp)
                                )
                                Spacer(modifier = Modifier.width(MimikyuSpacing.sm))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = localTrack.title,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = localTrack.artist,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                    } else {
                                        Icon(painter = painterResource(MimikyuIcons.Plus), contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(MimikyuSpacing.sm))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MimikyuSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedTracks.size} songs selected",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Row {
                    MimikyuTextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(MimikyuSpacing.sm))
                    MimikyuButton(
                        onClick = {
                            if (playlistNameInput.isNotBlank()) {
                                onCreate(playlistNameInput.trim(), selectedTracks.toList())
                            } else {
                                ToastManager.showToast("Please enter a playlist name!")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (selectedTracks.isEmpty()) "Create Empty" else "Create (${selectedTracks.size})",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// "Sort By"
// ────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySortMenuSheet(
    currentSortOrder: LibrarySortOrder,
    onSelect: (LibrarySortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = BottomSheetShape,
        dragHandle = { MimikyuDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = MimikyuSpacing.lg, vertical = MimikyuSpacing.md)
        ) {
            Text(
                text = "Sort By",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = MimikyuSpacing.md)
            )

            val sortOptions = listOf(
                LibrarySortOrder.RECENTLY_PLAYED to "Recently Played",
                LibrarySortOrder.NAME to "Alphabetical",
                LibrarySortOrder.CUSTOM to "Custom"
            )

            sortOptions.forEach { (order, label) ->
                val isSelected = currentSortOrder == order
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(order)
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
