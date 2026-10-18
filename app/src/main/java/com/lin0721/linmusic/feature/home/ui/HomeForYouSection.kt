package com.lin0721.linmusic.feature.home.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.lin0721.linmusic.core.ui.components.CoverPlaceholder
import com.lin0721.linmusic.core.ui.components.ToastManager
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.LayoutReflowDurationMs
import com.lin0721.linmusic.core.ui.theme.LocalMimikyuWindowSizeClass
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.MimikyuWindowSizeClass
import com.lin0721.linmusic.core.ui.theme.RobotoFontFamily
import com.lin0721.linmusic.core.ui.components.MimikyuPlaylistCover
import com.lin0721.linmusic.core.ui.components.MimikyuCoverType
import com.lin0721.linmusic.core.ui.components.MimikyuWireframePlaylistCard
import com.lin0721.linmusic.feature.home.data.DailySong
import com.lin0721.linmusic.feature.home.data.PersonalizedPlaylist
import com.lin0721.linmusic.feature.home.domain.ToplistInfo

private val CardWidthCompact = 150.dp
private val CardWidthExpanded = 180.dp

data class RecommendedPlaylistItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val coverUrl: String,
    val isAlbum: Boolean = false
)

val MIMIKYU_RECOMMENDED_PLAYLISTS = listOf(
    RecommendedPlaylistItem(
        id = -10L,
        title = "Daily Mix 1",
        subtitle = "Personalized for you",
        coverUrl = ""
    ),
    RecommendedPlaylistItem(
        id = -11L,
        title = "Daily Mix 2",
        subtitle = "Personalized for you",
        coverUrl = ""
    ),
    RecommendedPlaylistItem(
        id = -12L,
        title = "Daily Mix 3",
        subtitle = "Personalized for you",
        coverUrl = ""
    )
)

/**
 * Mimikyu For You Section
 */
@Composable
fun ForYouSection(
    dailySongs: List<DailySong> = emptyList(),
    toplists: List<ToplistInfo> = emptyList(),
    recommendPlaylists: List<PersonalizedPlaylist> = emptyList(),
    dailyMixes: List<com.lin0721.linmusic.core.player.recommendation.DailyMixPlaylist> = emptyList(),
    onDailyRecommendClick: () -> Unit = {},
    onHotlistClick: (Long) -> Unit = {},
    onIntelligenceClick: () -> Unit = {},
    onRadarClick: (Long) -> Unit = {},
    onRoamingClick: () -> Unit = {},
    onPlaylistClick: (Long, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val cardWidth by animateDpAsState(
        targetValue = if (LocalMimikyuWindowSizeClass.current == MimikyuWindowSizeClass.Expanded) {
            CardWidthExpanded
        } else {
            CardWidthCompact
        },
        animationSpec = tween(LayoutReflowDurationMs, easing = FastOutSlowInEasing),
        label = "featured_card_width"
    )

    // Build items prioritizing generated Daily Mixes and real user playlists only
    val displayItems = remember(dailyMixes, recommendPlaylists) {
        val mixItems = if (dailyMixes.isNotEmpty()) {
            dailyMixes.map { mix ->
                RecommendedPlaylistItem(
                    id = mix.id,
                    title = mix.title,
                    subtitle = mix.subtitle,
                    coverUrl = mix.coverUrl,
                    isAlbum = false
                )
            }
        } else {
            MIMIKYU_RECOMMENDED_PLAYLISTS
        }

        val otherItems = recommendPlaylists.map { p ->
            RecommendedPlaylistItem(
                id = p.id,
                title = p.name,
                subtitle = "Playlist • October",
                coverUrl = p.picUrl,
                isAlbum = false
            )
        }

        mixItems + otherItems
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MimikyuSpacing.lg)
    ) {
        // ── Section header: "Recommended for Today" + "Show all" ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = HomeEdgePadding, end = HomeEdgePadding, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recommended for Today",
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = RobotoFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )

            Text(
                text = "Show all",
                color = Color(0xFFB3B3B3),
                fontSize = 12.sp,
                fontFamily = RobotoFontFamily,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (displayItems.isNotEmpty()) {
                            val first = displayItems.first()
                            onPlaylistClick(first.id, first.isAlbum)
                        } else if (toplists.isNotEmpty()) {
                            onHotlistClick(toplists.first().id)
                        } else {
                            onDailyRecommendClick()
                        }
                    }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            )
        }

        // ──  and  ──
        LazyRow(
            contentPadding = PaddingValues(horizontal = HomeEdgePadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(displayItems, key = { index, item -> "${item.id}_$index" }) { _, item ->
                RecommendedPlaylistCard(
                    item = item,
                    cardWidth = cardWidth,
                    onClick = {
                        // Mimikyu ：/
                        onPlaylistClick(item.id, item.isAlbum)
                    }
                )
            }
        }
    }
}

/**
 * Mimikyu / ( 1:1 ，8dp ， and )
 */
@Composable
private fun RecommendedPlaylistCard(
    item: RecommendedPlaylistItem,
    cardWidth: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDailyMix = item.id in listOf(-1L, -2L, -3L, -10L, -11L, -12L) || item.title.startsWith("Daily Mix", ignoreCase = true)
    val dailyMixNum = Regex("""\d+""").find(item.title)?.value?.toIntOrNull() ?: when (item.id) {
        -1L, -10L -> 1
        -2L, -11L -> 2
        -3L, -12L -> 3
        else -> 1
    }
    val isGenreMix = item.title.endsWith("Mix", ignoreCase = true)
    val trackCount = when {
        isDailyMix -> if (dailyMixNum == 2) 32 else 50
        isGenreMix -> if (item.title.contains("Chill", ignoreCase = true)) 32 else 50
        else -> null
    }

    MimikyuWireframePlaylistCard(
        title = item.title,
        subtitle = item.subtitle,
        trackCount = trackCount,
        coverUrl = item.coverUrl,
        isDailyMix = isDailyMix,
        dailyMixNumber = dailyMixNum,
        isGenreMix = isGenreMix,
        cardWidth = cardWidth,
        onClick = onClick,
        modifier = modifier
    )
}
