package com.lin0721.linmusic.core.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

// ：，，
const val PressDownDurationMs = 90
const val PressUpDurationMs = 130

val PressDownSpec = tween<Float>(PressDownDurationMs, easing = LinearEasing)
val PressUpSpec = tween<Float>(PressUpDurationMs, easing = FastOutSlowInEasing)

// ：、，
const val DataEnterDurationMs = 700

// of ， of
const val DataEnterStaggerFraction = 0.4f

val DataEnterSpec = tween<Float>(DataEnterDurationMs, easing = FastOutSlowInEasing)

// （ /Cookie ）
const val ContentSwitchDurationMs = 300

// （/）
const val ScreenSlideDurationMs = 320

// Play：，，Collapse
val PanelRiseSpec = tween<Float>(300, easing = FastOutSlowInEasing)

// Play，： PanelReflowFadeFromAlpha
const val PanelReflowFadeFromAlpha = 0.5f
val PanelReflowFadeSpec = tween<Float>(150, easing = LinearEasing)

// Play and ： /
val PanelFullscreenSpec = tween<Float>(320, easing = FastOutSlowInEasing)

// Play、， and / of
const val LayoutReflowDurationMs = 300

// ：/， and PlayExpandCollapse
val SwipeCoverSpringSpec: SpringSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

// scale  of ；highlightAlpha  of （1f ），
// ，
@Immutable
data class PressStyle(val scale: Float, val highlightAlpha: Float)

// of ，
object MimikyuPress {

    val Card = PressStyle(scale = 0.96f, highlightAlpha = 0f)

    // /Chip
    val Pill = PressStyle(scale = 0.94f, highlightAlpha = 0f)


    val Icon = PressStyle(scale = 0.88f, highlightAlpha = 0f)


    val Action = PressStyle(scale = 0.97f, highlightAlpha = 0f)

    // /：，；
    val Row = PressStyle(scale = 0.99f, highlightAlpha = 1f)

    // tab
    val Tab = PressStyle(scale = 0.90f, highlightAlpha = 0f)

    // Play/Pause/
    val Transport = PressStyle(scale = 0.90f, highlightAlpha = 0f)

    // 、
    val None = PressStyle(scale = 1f, highlightAlpha = 0f)
}
