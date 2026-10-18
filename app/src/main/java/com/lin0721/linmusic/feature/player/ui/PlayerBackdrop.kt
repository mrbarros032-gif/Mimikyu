package com.lin0721.linmusic.feature.player.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.ui.theme.darken
import com.lin0721.linmusic.core.ui.theme.saturateIfChromatic

import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class BackdropMode { Collapsed, Immersive }

// Modifier.blur /
private val BACKDROP_BLUR_RADIUS = 60.dp

// of ： + lighten/darken ，Immersive  and Lyrics Preview
// fill  of ， of  base——base  Vibrant
// lightBlob  null  darkBlob
internal fun DrawScope.drawSingleHueMesh(
    fill: Color,
    lightBlob: Color? = null,
    lightCenter: Offset = Offset.Zero,
    lightRadius: Float = 0f,
    darkBlob: Color,
    darkCenter: Offset,
    darkRadius: Float
) {
    drawRect(color = fill)
    if (lightBlob != null) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(lightBlob.copy(alpha = 0.5f), Color.Transparent),
                center = lightCenter,
                radius = lightRadius
            ),
            center = lightCenter,
            radius = lightRadius
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(darkBlob.copy(alpha = 0.6f), Color.Transparent),
            center = darkCenter,
            radius = darkRadius
        ),
        center = darkCenter,
        radius = darkRadius
    )
}

private fun Color.withHsv(hShift: Float = 0f, sMult: Float = 1f, vSet: Float? = null): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    hsv[0] = (hsv[0] + hShift) % 360f
    if (hsv[0] < 0f) hsv[0] += 360f
    hsv[1] = (hsv[1] * sMult).coerceIn(0.20f, 1.0f)
    if (vSet != null) {
        hsv[2] = vSet.coerceIn(0.04f, 0.95f)
    }
    return Color(android.graphics.Color.HSVToColor(hsv))
}

// Play：Collapsed（Hero） alpha ，Immersive（），
// content  Box：，content（Immersive ）
@Composable
fun PlayerBackdrop(
    base: Color,
    mode: BackdropMode,
    modifier: Modifier = Modifier,
    translationYProvider: () -> Float = { 0f },
    content: @Composable BoxScope.() -> Unit = {}
) {
    when (mode) {
        BackdropMode.Collapsed -> {
            val density = LocalDensity.current
            val infiniteTransition = rememberInfiniteTransition(label = "bg_breathe")
            val gradientEndYDp by infiniteTransition.animateFloat(
                initialValue = 1050f,
                targetValue = 1150f,
                animationSpec = infiniteRepeatable(
                    animation = tween(8000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "gradient_end"
            )
            val gradientEndY = with(density) { gradientEndYDp.dp.toPx() }

            // base ，
            val fillColor = base

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(1200.dp)
                    .graphicsLayer { translationY = translationYProvider() }
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(BACKDROP_BLUR_RADIUS)
                        .drawBehind {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.0f to fillColor,
                                    0.2f to fillColor.copy(alpha = 0.75f),
                                    0.45f to fillColor.copy(alpha = 0.45f),
                                    0.62f to fillColor.copy(alpha = 0.08f),
                                    0.76f to Color.Transparent,
                                    startY = 0f,
                                    endY = gradientEndY
                                )
                            )
                        }
                )
                content()
            }
        }

        BackdropMode.Immersive -> {
            val infiniteTransition = rememberInfiniteTransition(label = "apple_music_lyrics_motion")

            val t1 by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Restart),
                label = "motion_t1"
            )
            val t2 by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing), RepeatMode.Restart),
                label = "motion_t2"
            )
            val t3 by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(34000, easing = LinearEasing), RepeatMode.Restart),
                label = "motion_t3"
            )
            val pulse by infiniteTransition.animateFloat(
                initialValue = 0.88f,
                targetValue = 1.14f,
                animationSpec = infiniteRepeatable(tween(11000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "motion_pulse"
            )

            // base  of  Apple Music
            val color1 = remember(base) { base.withHsv(hShift = 0f, sMult = 1.4f, vSet = 0.48f) }
            val color2 = remember(base) { base.withHsv(hShift = 45f, sMult = 1.3f, vSet = 0.44f) }
            val color3 = remember(base) { base.withHsv(hShift = -40f, sMult = 1.2f, vSet = 0.38f) }
            val color4 = remember(base) { base.withHsv(hShift = 20f, sMult = 1.0f, vSet = 0.25f) }
            val baseFill = remember(base) { base.withHsv(hShift = 0f, sMult = 0.85f, vSet = 0.10f) }

            Box(modifier = modifier) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(72.dp)
                        .drawBehind {
                            val w = size.width
                            val h = size.height
                            val minDim = size.minDimension

                            // 1.
                            drawRect(baseFill)

                            // 2. Primary Vibrant Orb ()
                            val a1 = t1 * 2f * PI.toFloat()
                            val cx1 = w * (0.34f + 0.18f * cos(a1))
                            val cy1 = h * (0.30f + 0.14f * sin(a1 * 1.25f))
                            val r1 = minDim * 0.90f * pulse
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(color1.copy(alpha = 0.80f), color1.copy(alpha = 0.35f), Color.Transparent),
                                    center = Offset(cx1, cy1),
                                    radius = r1
                                ),
                                center = Offset(cx1, cy1),
                                radius = r1
                            )

                            // 3. Harmonic Accent Orb ()
                            val a2 = t2 * 2f * PI.toFloat()
                            val cx2 = w * (0.68f + 0.18f * sin(a2))
                            val cy2 = h * (0.50f + 0.18f * cos(a2 * 0.9f))
                            val r2 = minDim * 0.95f * (2f - pulse)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(color2.copy(alpha = 0.75f), color2.copy(alpha = 0.30f), Color.Transparent),
                                    center = Offset(cx2, cy2),
                                    radius = r2
                                ),
                                center = Offset(cx2, cy2),
                                radius = r2
                            )

                            // 4. Ambient Glow Orb ()
                            val a3 = t3 * 2f * PI.toFloat()
                            val cx3 = w * (0.28f + 0.22f * sin(a3 * 0.8f))
                            val cy3 = h * (0.75f + 0.14f * cos(a3 * 1.1f))
                            val r3 = minDim * 0.85f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(color3.copy(alpha = 0.70f), color3.copy(alpha = 0.25f), Color.Transparent),
                                    center = Offset(cx3, cy3),
                                    radius = r3
                                ),
                                center = Offset(cx3, cy3),
                                radius = r3
                            )

                            // 5. Atmospheric Anchor Orb ()
                            val cx4 = w * (0.50f + 0.16f * cos(a2 * 1.2f))
                            val cy4 = h * (0.14f + 0.10f * sin(a1))
                            val r4 = minDim * 0.75f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(color4.copy(alpha = 0.65f), Color.Transparent),
                                    center = Offset(cx4, cy4),
                                    radius = r4
                                ),
                                center = Offset(cx4, cy4),
                                radius = r4
                            )

                            // 6. ， 100%
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.22f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.38f)
                                    )
                                )
                            )
                        }
                )
                content()
            }
        }
    }
}
