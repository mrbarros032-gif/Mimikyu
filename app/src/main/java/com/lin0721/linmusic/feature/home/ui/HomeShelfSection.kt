package com.lin0721.linmusic.feature.home.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.components.MimikyuWireframeAlbumCard
import com.lin0721.linmusic.core.ui.components.MimikyuWireframePlaylistCard
import com.lin0721.linmusic.core.ui.theme.LayoutReflowDurationMs
import com.lin0721.linmusic.core.ui.theme.LocalMimikyuWindowSizeClass
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.MimikyuWindowSizeClass
import com.lin0721.linmusic.core.ui.theme.RobotoFontFamily
import com.lin0721.linmusic.feature.home.domain.HomeCard
import com.lin0721.linmusic.feature.home.domain.HomeShelf

private val HorizontalCardWidthCompact = 150.dp
private val HorizontalCardWidthExpanded = 185.dp

internal val HomeEdgePadding = 20.dp

/**
 * Mimikyu-standard shelf section:
 * - Shelf title header
 * - Single horizontally scrollable row per shelf (consistent across all shelves)
 * - Exact Mimikyu wireframe cards (MimikyuStackedSleeve, rounded cover, title + count, subtitle)
 * - NO single songs with play icons
 */
@Composable
fun HomeShelfSection(
    shelf: HomeShelf,
    onCardClick: (HomeCard) -> Unit,
    modifier: Modifier = Modifier
) {
    if (shelf.cards.isEmpty()) return

    val windowSizeClass = LocalMimikyuWindowSizeClass.current
    val horizontalCardWidth by animateDpAsState(
        targetValue = if (windowSizeClass == MimikyuWindowSizeClass.Expanded) HorizontalCardWidthExpanded else HorizontalCardWidthCompact,
        animationSpec = tween(LayoutReflowDurationMs, easing = FastOutSlowInEasing),
        label = "shelf_horizontal_card_width"
    )

    Column(modifier = modifier.fillMaxWidth().padding(top = MimikyuSpacing.lg)) {
        // Shelf Header Title
        Text(
            text = shelf.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontFamily = RobotoFontFamily,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = HomeEdgePadding, end = HomeEdgePadding, bottom = 12.dp)
        )

        // 1 single horizontally scrollable section per shelf
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = HomeEdgePadding),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(
                items = shelf.cards,
                key = { index, card -> "${card::class.simpleName}_${card.id}_$index" }
            ) { _, card ->
                when (card) {
                    is HomeCard.Album -> {
                        MimikyuWireframeAlbumCard(
                            title = card.title,
                            artistName = card.caption,
                            coverUrl = card.coverUrl,
                            trackCount = card.trackCount,
                            isPinned = card.isPinned,
                            cardWidth = horizontalCardWidth,
                            onClick = { onCardClick(card) }
                        )
                    }
                    is HomeCard.Playlist -> {
                        val isDailyMix = card.isDailyMix || card.title.contains("Daily Mix", ignoreCase = true)
                        val dailyMixNum = Regex("""\d+""").find(card.title)?.value?.toIntOrNull() ?: 1
                        val isGenreMix = card.isGenreMix || card.title.endsWith("Mix", ignoreCase = true) || card.title.contains("Vibe", ignoreCase = true)
                        val isLikedSongs = card.title.equals("Liked Songs", ignoreCase = true)
                        val trackCount = card.trackCount ?: when {
                            isDailyMix -> if (dailyMixNum == 2) 32 else 50
                            isGenreMix -> if (card.title.contains("Chill", ignoreCase = true)) 32 else 50
                            isLikedSongs -> 12
                            card.title.contains("Rock", ignoreCase = true) -> 50
                            card.title.contains("Pop", ignoreCase = true) -> 50
                            else -> null
                        }

                        MimikyuWireframePlaylistCard(
                            title = card.title,
                            subtitle = card.caption,
                            trackCount = trackCount,
                            coverUrl = card.coverUrl,
                            isLikedSongs = isLikedSongs,
                            isDailyMix = isDailyMix,
                            dailyMixNumber = dailyMixNum,
                            isGenreMix = isGenreMix,
                            isPinned = card.isPinned,
                            cardWidth = horizontalCardWidth,
                            onClick = { onCardClick(card) }
                        )
                    }
                    else -> {
                        // Song / Voice fallback: rendered as album card wireframe without play button overlay
                        MimikyuWireframeAlbumCard(
                            title = card.title,
                            artistName = card.caption,
                            coverUrl = card.coverUrl,
                            cardWidth = horizontalCardWidth,
                            onClick = { onCardClick(card) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeShelfLoadingMore() {
    Box(
        modifier = Modifier.fillMaxWidth().height(72.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
