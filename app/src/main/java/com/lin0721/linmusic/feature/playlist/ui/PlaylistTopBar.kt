package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import com.lin0721.linmusic.core.ui.theme.MimikyuIcons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lin0721.linmusic.core.ui.components.MimikyuIconButton
import com.lin0721.linmusic.core.ui.interaction.pressScale
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.darken
import kotlin.math.roundToInt

// ────────────────────────────────────────────────────────────────────────────
// Overlay
// alpha=0（，）
// alpha  progress
// ────────────────────────────────────────────────────────────────────────────
@Composable
fun PlaylistTopBar(
    title: String,
    progress: Float,
    overlayHeight: Dp,
    statusBarHeight: Dp,
    dominantColor: Color,
    onBack: () -> Unit,
    onScrollToTop: () -> Unit = {}
) {
    val isScrolled = progress > 0f
    // ，； of ，
    val animatedDominant by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(800),
        label = "playlist_topbar_color"
    )
    val fillColor = remember(animatedDominant) { animatedDominant.darken(0.35f) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(overlayHeight)
            .background(fillColor.copy(alpha = progress))
            .zIndex(8f)
            // ，BackSongs
            .then(
                if (isScrolled) {
                    Modifier.pointerInput(onScrollToTop) {
                        detectTapGestures {
                            onScrollToTop()
                        }
                    }
                } else {
                    Modifier
                }
            )
            .padding(top = statusBarHeight)
    ) {
        // Sticky Mimikyu-style Back Button that appears when scrolling down
        val backAlpha = ((progress - 0.2f) / 0.3f).coerceIn(0f, 1f)
        if (backAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .alpha(backAlpha)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(MimikyuIcons.ArrowLeft),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        // Playlist Name：
        val titleAlpha = ((progress - 0.6f) / 0.4f).coerceIn(0f, 1f)
        val titleOffsetY = lerp(8.dp, 0.dp, titleAlpha)
        Text(
            text       = title,
            color      = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize   = 17.sp,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 60.dp)
                .offset(y = titleOffsetY)
                .alpha(titleAlpha)
        )
    }
}

// Play of "Shared Element"：Playlists of ，。
// provider  Float， offset{}  of
// ， of
@Composable
fun BoxScope.PlaylistDockedPlayButton(
    dockedOffsetYProvider: () -> Float,
    isCurrentlyPlayingThis: Boolean,
    onPlayAll: () -> Unit
) {
    val playInteraction = remember { MutableInteractionSource() }
    FloatingActionButton(
        onClick        = onPlayAll,
        containerColor = MaterialTheme.colorScheme.primary,
        shape          = CircleShape,
        interactionSource = playInteraction,
        modifier       = Modifier
            .pressScale(MimikyuPress.Transport, playInteraction)
            .align(Alignment.TopEnd)
            .padding(end = MimikyuSpacing.md)
            .offset { IntOffset(0, dockedOffsetYProvider().roundToInt()) }
            .size(48.dp)
            .zIndex(10f)
            .shadow(4.dp, CircleShape)
    ) {
        Icon(
            painter = painterResource(if (isCurrentlyPlayingThis) MimikyuIcons.Pause else MimikyuIcons.Play),
            contentDescription = if (isCurrentlyPlayingThis) "Pause" else "Play",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Songs
// ────────────────────────────────────────────────────────────────────────────
@Composable
fun PlaylistReorderTopBar(
    overlayHeight: Dp,
    statusBarHeight: Dp,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(overlayHeight)
            .background(MaterialTheme.colorScheme.background)
            .zIndex(8f)
            // ，
            .pointerInput(Unit) {
                detectTapGestures { }
            }
            .padding(top = statusBarHeight)
    ) {
        // Cancel
        com.lin0721.linmusic.core.ui.components.MimikyuTextButton(
            onClick = onCancel,
            enabled = !isSaving,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = MimikyuSpacing.sm)
        ) {
            Text(
                text = "Cancel",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp
            )
        }


        Text(
            text = "Reorder Songs",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        // / Save
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = MimikyuSpacing.sm),
            contentAlignment = Alignment.Center
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                com.lin0721.linmusic.core.ui.components.MimikyuTextButton(
                    onClick = onDone
                ) {
                    Text(
                        text = "Done",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

