package com.lin0721.linmusic.feature.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.player.domain.LyricLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource

// Spotify-style active lyric line above song title with fixed height to prevent element shifting
@Composable
fun MiniLyricLine(
    lyrics: List<LyricLine>,
    currentLyricIndex: Int,
    isPlaying: Boolean,
    onOpenFullScreen: () -> Unit = {}
) {
    val isPureMusic = lyrics.size == 1 && lyrics[0].text == "Instrumental"
    val currentLine = lyrics.getOrNull(currentLyricIndex)
    val currentText = when {
        isPureMusic -> ""
        currentLine != null && !currentLine.isGap && currentLine.text.isNotBlank() -> currentLine.text
        else -> ""
    }

    // Always maintain a fixed height container so song title, controls and progress bar NEVER move or jump
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = MimikyuSpacing.lg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenFullScreen
            ),
        contentAlignment = Alignment.TopStart
    ) {
        if (currentText.isNotBlank()) {
            AnimatedContent(
                targetState = currentText,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "active_lyric"
            ) { text ->
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// of ，Pause
@Composable
fun LoadingDotsAnimation(
    color: Color,
    isPlaying: Boolean,
    dotSize: androidx.compose.ui.unit.Dp = 8.dp,
    spacing: androidx.compose.ui.unit.Dp = 6.dp
) {
    val dot1Scale = remember { Animatable(0.3f) }
    val dot2Scale = remember { Animatable(0.3f) }
    val dot3Scale = remember { Animatable(0.3f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            launch {
                dot1Scale.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            launch {
                delay(150)
                dot2Scale.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            launch {
                delay(300)
                dot3Scale.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
        } else {
            launch { dot1Scale.animateTo(0.3f, tween(200)) }
            launch { dot2Scale.animateTo(0.3f, tween(200)) }
            launch { dot3Scale.animateTo(0.3f, tween(200)) }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer {
                    scaleX = dot1Scale.value
                    scaleY = dot1Scale.value
                    alpha = dot1Scale.value
                }
                .background(color, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer {
                    scaleX = dot2Scale.value
                    scaleY = dot2Scale.value
                    alpha = dot2Scale.value
                }
                .background(color, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer {
                    scaleX = dot3Scale.value
                    scaleY = dot3Scale.value
                    alpha = dot3Scale.value
                }
                .background(color, CircleShape)
        )
    }
}
