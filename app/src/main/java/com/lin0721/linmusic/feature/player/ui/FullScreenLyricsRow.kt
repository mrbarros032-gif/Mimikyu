package com.lin0721.linmusic.feature.player.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.isActive
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.player.domain.LyricLine
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.SFProFontFamily

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
// accompanist-lyrics-ui Ã©Â£Å½Ã¦Â Â¼Ã¦Â­Å’Ã¨Â¯ÂÃ¨Â¡Å’Ã¯Â¼Å¡Ã¥Â¼Â¹Ã¦â‚¬Â§Ã§Â¼Â©Ã¦â€Â¾ + Ã¦Â¸ÂÃ¨Â¿â€ºÃ¥Â¼ÂÃ¦Â¨Â¡Ã§Â³Å  + Ã¥ÂÂ¡Ã¦â€¹â€°OKÃ¦ÂµÂÃ¥â€¦â€°Ã©Â«ËœÃ¤ÂºÂ®
// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

@Composable
fun FullScreenLyricsRow(
    index: Int,
    line: LyricLine,
    isCurrent: Boolean,
    isCenterTarget: Boolean,
    distance: Int,
    highlightColor: Color,
    currentPositionProvider: () -> Long,
    fontSize: Int = 26,
    alignment: String = "left",
    secondaryMode: String = "translation",
    secondarySpacing: Int = 6,
    advancedKaraokeEffect: Boolean = true,
    isPlaying: Boolean = true,
    onTextBoundsInRoot: ((Rect) -> Unit)? = null,
    onClick: () -> Unit
) {
    val textBounds = if (onTextBoundsInRoot != null) remember { LyricTextBounds() } else null
    if (onTextBoundsInRoot != null) {
        DisposableEffect(Unit) {
            onDispose { onTextBoundsInRoot(Rect.Zero) }
        }
    }
    val reportMainBounds: Modifier = if (textBounds != null && onTextBoundsInRoot != null) {
        Modifier.onGloballyPositioned { coords ->
            textBounds.main = textBounds.mainLayout?.let { inkBoundsInRoot(it, coords) } ?: coords.boundsInRoot()
            onTextBoundsInRoot(textBounds.union())
        }
    } else Modifier
    val reportSecondaryBounds: Modifier = if (textBounds != null && onTextBoundsInRoot != null) {
        Modifier.onGloballyPositioned { coords ->
            textBounds.secondary = textBounds.secondaryLayout?.let { inkBoundsInRoot(it, coords) }
            onTextBoundsInRoot(textBounds.union())
        }
    } else Modifier

    val isOpposite = line.isOppositeSide
    val effectiveAlignment = if (isOpposite) "right" else alignment

    val textAlign = when (effectiveAlignment) {
        "center" -> TextAlign.Center
        "right" -> TextAlign.End
        else -> TextAlign.Start
    }
    val horizontalAlignment = when (effectiveAlignment) {
        "center" -> Alignment.CenterHorizontally
        "right" -> Alignment.End
        else -> Alignment.Start
    }
    val targetTransformOrigin = when (effectiveAlignment) {
        "center" -> TransformOrigin(0.5f, 0.5f)
        "right" -> TransformOrigin(1f, 0.5f)
        else -> TransformOrigin(0f, 0.5f)
    }
    val mainFontSize = fontSize.sp
    val translationFontSize = (fontSize - 5).coerceAtLeast(12).sp
    val mainLineHeight = (fontSize * 1.25f).sp
    val translationLineHeight = (translationFontSize.value * 1.35f).sp
    val spacingBetween = secondarySpacing.coerceAtLeast(0).dp

    // Apple Music 3D  and
    val targetScale = when {
        !line.isSynced -> 1.0f
        isCurrent && !line.isGap -> 1.04f
        isCenterTarget && !line.isGap -> 0.98f
        else -> when (distance) {
            1 -> 0.96f
            2 -> 0.92f
            else -> 0.88f
        }
    }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 240f),
        label = "fs_lyric_scale_$index"
    )

    val targetAlpha = when {
        isCurrent -> 1.0f
        else -> {
            // Current（ and ） 50% (0.50f) ： 1  50%， 2  42%， 3  34% ...  10%
            val calculated = 0.50f - ((distance - 1).coerceAtLeast(0) * 0.08f)
            calculated.coerceIn(0.10f, 0.50f)
        }
    }
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 240f),
        label = "fs_lyric_alpha_$index"
    )

    val paddingStart = when (effectiveAlignment) {
        "center" -> 24.dp
        "right" -> 64.dp
        else -> 24.dp
    }
    val paddingEnd = when (effectiveAlignment) {
        "center" -> 24.dp
        "right" -> 24.dp
        else -> if (isOpposite) 64.dp else 24.dp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = paddingStart, end = paddingEnd)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = animatedAlpha
                transformOrigin = targetTransformOrigin
            }
            .pressable(MimikyuPress.None) {
                onClick()
            },
        horizontalAlignment = horizontalAlignment
    ) {
        if (line.isGap) {
            InstrumentalGapRow(
                line = line,
                currentPositionProvider = currentPositionProvider,
                highlightColor = highlightColor,
                alignment = effectiveAlignment,
                modifier = reportMainBounds
            )
        } else if (isCurrent && line.text.isNotBlank()) {
            textBounds?.mainLayout = null
            Box(modifier = reportMainBounds) {
                KaraokeLyricRow(
                    line = line,
                    currentPositionProvider = currentPositionProvider,
                    inactiveColor = highlightColor.copy(alpha = 0.45f),
                    activeColor = Color.White,
                    fontSize = mainFontSize,
                    lineHeight = mainLineHeight,
                    textAlign = textAlign,
                    advancedEffect = advancedKaraokeEffect,
                    isPlaying = isPlaying
                )
            }
        } else {
            Text(
                text = line.text,
                fontSize = mainFontSize,
                lineHeight = mainLineHeight,
                color = Color.White,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                fontFamily = SFProFontFamily,
                letterSpacing = (-0.5).sp,
                textAlign = textAlign,
                onTextLayout = { textBounds?.mainLayout = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(reportMainBounds)
            )
        }
        val secondaryText = when (secondaryMode) {
            "translation" -> line.translation
            "roma" -> line.roma
            else -> null
        }
        if (secondaryText != null) {
            Spacer(modifier = Modifier.height(spacingBetween))
            Text(
                text = secondaryText,
                fontSize = translationFontSize,
                lineHeight = translationLineHeight,
                color = if (isCurrent) Color.White else highlightColor,
                textAlign = textAlign,
                onTextLayout = { textBounds?.secondaryLayout = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(reportSecondaryBounds)
            )
        } else {
            textBounds?.secondary = null
        }
    }
}

private class LyricTextBounds {
    var mainLayout: TextLayoutResult? = null
    var secondaryLayout: TextLayoutResult? = null
    var main: Rect? = null
    var secondary: Rect? = null

    fun union(): Rect {
        val a = main
        val b = secondary
        return when {
            a != null && b != null -> Rect(
                left = minOf(a.left, b.left),
                top = minOf(a.top, b.top),
                right = maxOf(a.right, b.right),
                bottom = maxOf(a.bottom, b.bottom)
            )
            else -> a ?: b ?: Rect.Zero
        }
    }
}

private fun inkBoundsInRoot(layout: TextLayoutResult, coords: LayoutCoordinates): Rect {
    if (layout.lineCount == 0) return Rect.Zero
    var left = Float.MAX_VALUE
    var right = 0f
    for (line in 0 until layout.lineCount) {
        left = minOf(left, layout.getLineLeft(line))
        right = maxOf(right, layout.getLineRight(line))
    }
    return Rect(
        topLeft = coords.localToRoot(Offset(left, 0f)),
        bottomRight = coords.localToRoot(Offset(right, layout.size.height.toFloat()))
    )
}

@Composable
private fun InstrumentalGapRow(
    line: LyricLine,
    currentPositionProvider: () -> Long,
    highlightColor: Color,
    alignment: String = "left",
    modifier: Modifier = Modifier
) {
    var frameTick by remember(line) { mutableLongStateOf(0L) }

    LaunchedEffect(line) {
        while (isActive) {
            withFrameNanos { frameNano ->
                frameTick = frameNano
            }
        }
    }

    val currentPos = currentPositionProvider()
    val isActiveGap = currentPos in line.timeMs..line.endTimeMs
    val remainingMs = (line.endTimeMs - currentPos).coerceAtLeast(0L)

    val horizontalAlign = when (alignment) {
        "center" -> Alignment.CenterHorizontally
        "right" -> Alignment.End
        else -> Alignment.Start
    }

    // 3: Beat 0s  Dot 3 ， 180ms alpha exit
    val exitAlpha = if (remainingMs in 0L..180L && isActiveGap) {
        (remainingMs / 180f).coerceIn(0f, 1f)
    } else 1.0f

    val now = System.currentTimeMillis()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .graphicsLayer {
                val unused = frameTick
                alpha = exitAlpha
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, horizontalAlign)
    ) {
        for (dotIndex in 0..2) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        val unused = frameTick
                        // ，220ms  Dot 1 -> Dot 2 -> Dot 3
                        val delayMs = dotIndex * 220L
                        val phase = ((now - delayMs) % 1800L).toDouble() / 1800.0 * (2.0 * Math.PI)
                        val wave = ((kotlin.math.sin(phase) + 1.0) / 2.0).toFloat() // 0.0f .. 1.0f
                        val dotScale = 1.0f + 0.08f * wave
                        val dotAlpha = 0.70f + 0.30f * wave
                        scaleX = dotScale
                        scaleY = dotScale
                        alpha = dotAlpha
                    }
                    .size(9.5.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

