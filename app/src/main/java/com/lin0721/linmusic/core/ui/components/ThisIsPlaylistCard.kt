package com.lin0721.linmusic.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.lin0721.linmusic.core.player.QueueItem
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.LayoutReflowDurationMs
import com.lin0721.linmusic.core.ui.theme.LocalMimikyuWindowSizeClass
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.MimikyuWindowSizeClass
import com.lin0721.linmusic.core.ui.theme.RobotoFontFamily

private val ShelfCardWidthCompact = 165.dp
private val ShelfCardWidthExpanded = 185.dp
private val HomeEdgePadding = 16.dp

/**
 * Specifications for Mimikyu-tier "THIS IS" & "Made For Your Vibe" cards with artist cutouts.
 */
data class ThisIsCoverSpec(
    val id: String,
    val headline: String, // e.g., "MY THERAPIST", "THERAPY", "MY TASTE IN WOMEN"
    val subtitle: String, // Shown below the card on the shelf
    val artistCutoutUrlOrBitmap: Any?, // Transparent PNG URL or ML Kit Bitmap
    val bottomColorStart: Color, // e.g., Color(0xFF00AAFF) or Color(0xFFFFB300)
    val bottomColorEnd: Color = bottomColorStart, // Optional gradient
    val virtualId: Long = 0L,
    val artistName: String = "",
    val tracks: List<QueueItem> = emptyList()
)

/**
 * Mimikyu-grade 3-layer overlap effect card:
 * - Layer 1: Split background (white top 38%, solid/gradient color bottom 62%)
 * - Layer 2: Top-left circular badge with subtle drop shadow
 * - Layer 3: Bold centered typography ("THIS IS [HEADLINE]") in the white header
 * - Layer 4: Cutout PNG of the artist whose head breaks past the color line into the white area
 */
@Composable
fun ThisIsPlaylistCard(
    spec: ThisIsCoverSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = ShelfCardWidthCompact
) {
    Column(
        modifier = modifier
            .width(cardWidth)
            .pressable(
                style = MimikyuPress.Card,
                onClick = onClick
            )
    ) {
        // 1:1 Square Card
        Box(
            modifier = Modifier
                .size(cardWidth)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
        ) {
            // LAYER 1: Split Background (Top 38% White, Bottom 62% Color/Gradient)
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.38f)
                        .background(Color.White)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.62f)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(spec.bottomColorStart, spec.bottomColorEnd)
                            )
                        )
                )
            }

            // LAYER 2: Top-Left Circular App Logo Badge (with subtle drop shadow)
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1DB954))
                )
            }

            // LAYER 3: "THIS IS" + Bold Personal Title
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, start = 8.dp, end = 8.dp)
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "THIS IS",
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = spec.headline.uppercase(),
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = (-0.5).sp
                )
            }

            // LAYER 4: Artist Cutout (Positioned at bottom, head overlaps the 38% line!)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(spec.artistCutoutUrlOrBitmap)
                    .crossfade(true)
                    .build(),
                contentDescription = spec.headline,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.74f) // 74% height makes the head cross into the 38% white top!
                    .align(Alignment.BottomCenter)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Shelf Card Title & Subtitle
        Text(
            text = "This Is ${spec.headline.lowercase().replaceFirstChar { it.uppercase() }}",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = spec.subtitle,
            color = Color(0xFFB3B3B3),
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * "Made for your vibe" horizontal shelf row on the Home Screen.
 */
@Composable
fun MadeForYourVibeShelf(
    items: List<ThisIsCoverSpec>,
    onCardClick: (Long) -> Unit,
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
        label = "vibe_shelf_width"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MimikyuSpacing.lg)
    ) {
        // Shelf Title Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = HomeEdgePadding, end = HomeEdgePadding, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Made for your vibe",
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = RobotoFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
        }

        // Horizontal card list
        LazyRow(
            contentPadding = PaddingValues(horizontal = HomeEdgePadding),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(items, key = { index, item -> "${item.id}_$index" }) { _, item ->
                ThisIsPlaylistCard(
                    spec = item,
                    cardWidth = cardWidth,
                    onClick = { onCardClick(item.virtualId) }
                )
            }
        }
    }
}
