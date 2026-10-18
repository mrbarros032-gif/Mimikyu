package com.lin0721.linmusic.feature.home.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lin0721.linmusic.core.player.recommendation.DailyMixPlaylist
import com.lin0721.linmusic.core.ui.components.MimikyuCoverType
import com.lin0721.linmusic.core.ui.components.MimikyuPlaylistCover
import com.lin0721.linmusic.core.ui.components.MimikyuWireframePlaylistCard
import com.lin0721.linmusic.core.ui.components.MimikyuWireframeArtistCard
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.LayoutReflowDurationMs
import com.lin0721.linmusic.core.ui.theme.LocalMimikyuWindowSizeClass
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.MimikyuWindowSizeClass
import com.lin0721.linmusic.core.ui.theme.RobotoFontFamily

private val ShelfCardWidthCompact = 150.dp
private val ShelfCardWidthExpanded = 180.dp
private val ArtistCircleWidthCompact = 130.dp
private val ArtistCircleWidthExpanded = 150.dp

/**
 * Reusable Mimikyu shelf row with header title and horizontal card list.
 */
@Composable
fun MimikyuMixesShelf(
    title: String,
    items: List<DailyMixPlaylist>,
    onPlaylistClick: (Long, Boolean) -> Unit,
    onArtistClick: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val cardWidth by animateDpAsState(
        targetValue = if (LocalMimikyuWindowSizeClass.current == MimikyuWindowSizeClass.Expanded) {
            ShelfCardWidthExpanded
        } else {
            ShelfCardWidthCompact
        },
        animationSpec = tween(LayoutReflowDurationMs, easing = FastOutSlowInEasing),
        label = "mix_shelf_width"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MimikyuSpacing.lg)
    ) {
        // Shelf Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = HomeEdgePadding, end = HomeEdgePadding, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = RobotoFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
        }

        // Horizontal items
        LazyRow(
            contentPadding = PaddingValues(horizontal = HomeEdgePadding),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(items, key = { index, item -> "${item.id}_$index" }) { _, item ->
                MimikyuPlaylistCard(
                    item = item,
                    cardWidth = cardWidth,
                    onClick = {
                        if (item.isArtistProfile && onArtistClick != null) {
                            onArtistClick(item.artistId ?: 0L)
                        } else {
                            onPlaylistClick(item.id, false)
                        }
                    }
                )
            }
        }
    }
}

/**
 * "More like [Artist]" shelf with artist avatar in the header.
 */
@Composable
fun MimikyuMoreLikeShelf(
    artistName: String,
    artistCover: String,
    items: List<DailyMixPlaylist>,
    onPlaylistClick: (Long, Boolean) -> Unit,
    onArtistClick: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val cardWidth by animateDpAsState(
        targetValue = if (LocalMimikyuWindowSizeClass.current == MimikyuWindowSizeClass.Expanded) {
            ShelfCardWidthExpanded
        } else {
            ShelfCardWidthCompact
        },
        animationSpec = tween(LayoutReflowDurationMs, easing = FastOutSlowInEasing),
        label = "more_like_width"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MimikyuSpacing.lg)
    ) {
        // Header with small circular artist avatar + "More like [Artist]"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = HomeEdgePadding, end = HomeEdgePadding, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (artistCover.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artistCover)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
            }

            Column {
                Text(
                    text = "MORE LIKE",
                    color = Color(0xFFB3B3B3),
                    fontSize = 11.sp,
                    fontFamily = RobotoFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = artistName.ifBlank { "Recommended" },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = RobotoFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = HomeEdgePadding),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(items, key = { index, item -> "${item.id}_$index" }) { _, item ->
                if (item.isArtistProfile) {
                    MimikyuArtistCircleCard(
                        item = item,
                        cardWidth = cardWidth - 20.dp,
                        onClick = {
                            if (onArtistClick != null) {
                                onArtistClick(item.artistId ?: 0L)
                            } else {
                                onPlaylistClick(item.id, false)
                            }
                        }
                    )
                } else {
                    MimikyuPlaylistCard(
                        item = item,
                        cardWidth = cardWidth,
                        onClick = { onPlaylistClick(item.id, false) }
                    )
                }
            }
        }
    }
}

/**
 * "Your favourite artists" circular card shelf.
 */
@Composable
fun MimikyuFavoriteArtistsShelf(
    items: List<DailyMixPlaylist>,
    onArtistClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val cardWidth by animateDpAsState(
        targetValue = if (LocalMimikyuWindowSizeClass.current == MimikyuWindowSizeClass.Expanded) {
            ArtistCircleWidthExpanded
        } else {
            ArtistCircleWidthCompact
        },
        animationSpec = tween(LayoutReflowDurationMs, easing = FastOutSlowInEasing),
        label = "artist_circle_width"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MimikyuSpacing.lg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = HomeEdgePadding, end = HomeEdgePadding, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Artists you might like",
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = RobotoFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = HomeEdgePadding),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(items, key = { index, item -> "${item.id}_$index" }) { _, item ->
                MimikyuArtistCircleCard(
                    item = item,
                    cardWidth = cardWidth,
                    onClick = { onArtistClick(item.artistId ?: 0L) }
                )
            }
        }
    }
}

@Composable
private fun MimikyuPlaylistCard(
    item: DailyMixPlaylist,
    cardWidth: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDailyMix = item.coverType == MimikyuCoverType.DAILY_MIX
    val dailyMixNum = Regex("""\d+""").find(item.title)?.value?.toIntOrNull() ?: 1
    val isGenreMix = item.coverType == MimikyuCoverType.VIBE_MIX
    val isLikedSongs = item.title.equals("Liked Songs", ignoreCase = true)

    val trackCount = item.tracks.size.takeIf { it > 0 } ?: when {
        isDailyMix -> if (dailyMixNum == 2) 32 else 50
        isGenreMix -> if (item.title.contains("Chill", ignoreCase = true)) 32 else 50
        else -> null
    }

    MimikyuWireframePlaylistCard(
        title = item.title,
        subtitle = item.subtitle,
        trackCount = trackCount,
        coverUrl = item.coverUrl,
        isLikedSongs = isLikedSongs,
        isDailyMix = isDailyMix,
        dailyMixNumber = dailyMixNum,
        isGenreMix = isGenreMix,
        accentColor = item.primaryColor,
        cardWidth = cardWidth,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun MimikyuArtistCircleCard(
    item: DailyMixPlaylist,
    cardWidth: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MimikyuWireframeArtistCard(
        name = item.title,
        avatarUrl = item.coverUrl,
        circleSize = cardWidth,
        onClick = onClick,
        modifier = modifier
    )
}
