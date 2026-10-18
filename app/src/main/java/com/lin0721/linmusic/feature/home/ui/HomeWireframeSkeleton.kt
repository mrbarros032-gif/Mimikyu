package com.lin0721.linmusic.feature.home.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.theme.RobotoFontFamily

/**
 * Premium Spotify/Apple Music-grade wireframe skeleton shimmer animation.
 * Replaces frozen blank screens with dynamic pulse animations on weak internet.
 */
@Composable
fun Modifier.homeShimmer(shape: Shape = RoundedCornerShape(8.dp)): Modifier {
    val transition = rememberInfiniteTransition(label = "home_wireframe_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -800f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "home_wireframe_translate"
    )

    val baseColor = Color(0xFF161616)
    val highlightColor = Color(0xFF2C2C2C)
    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim, 0f),
        end = Offset(translateAnim + 500f, 0f)
    )

    return this
        .clip(shape)
        .background(brush = brush, shape = shape)
}

@Composable
fun HomeWireframeSkeleton(
    modifier: Modifier = Modifier,
    isWeakConnection: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HomeEdgePadding)
    ) {
        // Subtle status indicator when connection is slow
        if (isWeakConnection) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFFFB300), CircleShape)
                )
                Text(
                    text = "Slow connection detected • Loading playlists...",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontFamily = RobotoFontFamily,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 1. Quick Access 2-Column Grid Skeleton (6 items)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WireframeQuickAccessCard(modifier = Modifier.weight(1f))
                    WireframeQuickAccessCard(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 2. Shelf 1 Wireframe: "Your top mixes"
        WireframeShelfSection(titleWidth = 150)

        Spacer(modifier = Modifier.height(28.dp))

        // 3. Shelf 2 Wireframe: "Recommended for you"
        WireframeShelfSection(titleWidth = 200)

        Spacer(modifier = Modifier.height(28.dp))

        // 4. Artist Circles Wireframe: "Artists you might like"
        WireframeArtistsShelf()
    }
}

@Composable
private fun WireframeQuickAccessCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(56.dp)
            .background(Color(0xFF141414), RoundedCornerShape(8.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail square
        Box(
            modifier = Modifier
                .size(56.dp)
                .homeShimmer(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Text bar placeholder
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(13.dp)
                    .homeShimmer(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun WireframeShelfSection(titleWidth: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Title placeholder
        Box(
            modifier = Modifier
                .width(titleWidth.dp)
                .height(22.dp)
                .homeShimmer(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Row of 3 Playlist Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            repeat(3) {
                Column(
                    modifier = Modifier.width(150.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card sleeve square
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .homeShimmer(RoundedCornerShape(8.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                    )

                    // Title line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(14.dp)
                            .homeShimmer(RoundedCornerShape(4.dp))
                    )

                    // Subtitle line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(12.dp)
                            .homeShimmer(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun WireframeArtistsShelf() {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(22.dp)
                .homeShimmer(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Circles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(3) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(125.dp)
                            .homeShimmer(CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(13.dp)
                            .homeShimmer(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
