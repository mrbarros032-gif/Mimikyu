package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.lin0721.linmusic.core.ui.theme.MimikyuIcons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.components.MimikyuIconButton
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.ui.components.SongRow
import com.lin0721.linmusic.core.ui.components.SongRowData
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

// ────────────────────────────────────────────────────────────────────────────
// RecommendedSongs（ + Playlists of Recommended）
// ────────────────────────────────────────────────────────────────────────────
fun LazyListScope.playlistRecommendItems(
    recommendedSongs: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onRefreshRecommendations: () -> Unit,
    onPlaySong: (Track) -> Unit,
    onAddRecommendSong: (Track) -> Unit
) {
    item(key = "recommendation_header") {
        RecommendationHeader(
            onRefresh = onRefreshRecommendations
        )
    }
    items(recommendedSongs, key = { "rec_${it.id}" }) { track ->
        SongRow(
            data = SongRowData(
                id = track.id,
                title = track.name,
                artist = track.ar.joinToString(" • ") { it.name },
                coverUrl = track.al.picUrl
            ),
            isActive = currentTrackId == track.id.toString(),
            isPlaying = isPlaying,
            onClick = { onPlaySong(track) },
            trailingSlot = {
                MimikyuIconButton(
                    onClick = { onAddRecommendSong(track) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(MimikyuIcons.Plus),
                        contentDescription = "Add song to playlist",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .padding(MimikyuSpacing.xxs)
                    )
                }
            }
        )
    }
}

// Recommended
@Composable
private fun RecommendationHeader(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MimikyuSpacing.md, vertical = MimikyuSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Recommended Songs",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .pressable(MimikyuPress.Pill, onClick = onRefresh)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(MimikyuIcons.Enhance),
                contentDescription = "Refresh recommendations",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(MimikyuSpacing.xs))
            Text(
                text = "Refresh",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// "You might also like" Section (Mimikyu 2-Column Grid as shown in phone screenshot)
// ────────────────────────────────────────────────────────────────────────────

data class CuratedMixItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val coverUrl: String = "",
    val isAlbum: Boolean = false,
    val coverType: com.lin0721.linmusic.core.ui.components.MimikyuCoverType = com.lin0721.linmusic.core.ui.components.MimikyuCoverType.DAILY_MIX
)

fun LazyListScope.playlistYouMightLikeSection(
    items: List<CuratedMixItem>,
    onItemClick: (Long, Boolean) -> Unit
) {
    if (items.isEmpty()) return

    item(key = "you_might_also_like_header") {
        Text(
            text = "You might also like",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 14.dp)
        )
    }

    val chunked = items.chunked(2)
    items(
        count = chunked.size,
        key = { index -> "you_might_like_row_$index" }
    ) { index ->
        val rowPair = chunked[index]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            rowPair.forEach { card ->
                Box(modifier = Modifier.weight(1f)) {
                    CuratedMixCardView(card = card, onClick = { onItemClick(card.id, card.isAlbum) })
                }
            }
            if (rowPair.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CuratedMixCardView(
    card: CuratedMixItem,
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (card.coverUrl.isNotBlank()) {
                coil.compose.SubcomposeAsyncImage(
                    model = card.coverUrl,
                    contentDescription = card.title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    loading = { com.lin0721.linmusic.core.ui.components.CoverPlaceholder() },
                    error = { com.lin0721.linmusic.core.ui.components.CoverPlaceholder() },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                com.lin0721.linmusic.core.ui.components.CoverPlaceholder()
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = card.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(2.dp))

        val formattedSubtitle = androidx.compose.runtime.remember(card.subtitle, card.isAlbum) {
            val clean = card.subtitle.trim()
            when {
                clean.isBlank() -> if (card.isAlbum) "Album" else "Playlist"
                clean.startsWith("Playlist", ignoreCase = true) || clean.startsWith("Album", ignoreCase = true) -> clean
                card.isAlbum -> "Album • $clean"
                else -> "Playlist • $clean"
            }
        }

        Text(
            text = formattedSubtitle,
            color = com.lin0721.linmusic.core.ui.theme.TextGray,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )
    }
}
