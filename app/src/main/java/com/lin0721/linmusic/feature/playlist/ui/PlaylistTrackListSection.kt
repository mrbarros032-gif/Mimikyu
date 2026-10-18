package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.theme.MimikyuIcons
import com.lin0721.linmusic.core.ui.components.MimikyuIconButton
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.ui.components.SongRow
import com.lin0721.linmusic.core.ui.components.SongRowData
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

// Playlists of ： and  Library  of
private val trackPlacementSpec = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)
private val trackFadeInSpec = spring<Float>(stiffness = Spring.StiffnessMedium)
private val trackFadeOutSpec = spring<Float>(stiffness = Spring.StiffnessMedium)

// ────────────────────────────────────────────────
// Songs：Search
// ────────────────────────────────────────────────
fun LazyListScope.playlistTrackItems(
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    likedSongIds: Set<Long> = emptySet(),
    isLoggedIn: Boolean = false,
    onPlaySong: (Track) -> Unit,
    onLikeClick: (Long) -> Unit = {},
    onOpenCollectSheet: (Long) -> Unit = {},
    onMoreClick: (Track) -> Unit,
    trackPlayCounts: Map<Long, Int> = emptyMap()
) {
    itemsIndexed(tracks, key = { index, track -> "${track.id}_$index" }) { _, track ->
        // SongRow  modifier， Box  animateItem：
        // key 、，
        Box(modifier = Modifier.animateItem(trackFadeInSpec, trackPlacementSpec, trackFadeOutSpec)) {
            SongRow(
                data = SongRowData(
                    id = track.id,
                    title = track.name,
                    artist = track.ar.joinToString(", ") { it.name },
                    coverUrl = track.al.picUrl
                ),
                isActive = currentTrackId == track.id.toString(),
                isPlaying = isPlaying,
                onClick = { onPlaySong(track) },
                trailingSlot = {
                    val playCount = trackPlayCounts[track.id]
                    if (playCount != null) {
                        Text(
                            text = "${playCount} plays",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = MimikyuSpacing.xs)
                        )
                    }
                    MimikyuIconButton(
                        onClick = { onMoreClick(track) },
                        modifier = Modifier.size(36.dp).padding(end = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(MimikyuIcons.More),
                            contentDescription = "More options",
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
    }
}
