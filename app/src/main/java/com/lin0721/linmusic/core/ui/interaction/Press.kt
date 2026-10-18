package com.lin0721.linmusic.core.ui.interaction

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.PressDownSpec
import com.lin0721.linmusic.core.ui.theme.PressStyle
import com.lin0721.linmusic.core.ui.theme.PressUpSpec
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ：，。

class MimikyuPressIndication(
    private val pressedScale: Float,
    private val dimAlpha: Float
) : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressHighlightNode(interactionSource, pressedScale, dimAlpha)

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is MimikyuPressIndication &&
                other.pressedScale == pressedScale &&
                other.dimAlpha == dimAlpha)

    override fun hashCode(): Int = 31 * pressedScale.hashCode() + dimAlpha.hashCode()

    companion object {
        // LocalIndication  of ，
        val Default = MimikyuPressIndication(MimikyuPress.Row.scale, MimikyuPress.Row.highlightAlpha)
    }
}

private class PressHighlightNode(
    private val interactionSource: InteractionSource,
    private val pressedScale: Float,
    private val dimAlpha: Float
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private val scale = Animatable(1f)
    private val dim = Animatable(0f)
    private var scaleJob: Job? = null
    private var dimJob: Job? = null

    override fun onAttach() {
        val longPressDelayMs = currentValueOf(LocalViewConfiguration).longPressTimeoutMillis
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        scaleJob?.cancel()
                        scaleJob = coroutineScope.launch { scale.animateTo(pressedScale, PressDownSpec) }
                        dimJob?.cancel()
                        // ，
                        dimJob = coroutineScope.launch {
                            delay(longPressDelayMs)
                            dim.animateTo(dimAlpha, PressDownSpec)
                        }
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        // Cancel of ，
                        scaleJob?.cancel()
                        scaleJob = coroutineScope.launch { scale.animateTo(1f, PressUpSpec) }
                        dimJob?.cancel()
                        dimJob = coroutineScope.launch { dim.animateTo(0f, PressUpSpec) }
                    }
                    else -> Unit
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val currentScale = scale.value
        val currentDim = dim.value
        scale(currentScale, currentScale) {
            // drawContent ： of  of ，
            if (currentDim > 0f) drawRect(color = Color.Black, alpha = currentDim)
            this@draw.drawContent()
        }
    }
}

// +  +  of 。
//
// clip / background ， and 。 shape  modifier
// ， of  clip ，：
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Modifier.pressable(
    style: PressStyle = MimikyuPress.Row,
    shape: Shape = RectangleShape,
    enabled: Boolean = true,
    role: Role? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return if (onLongClick != null) {
        this
            .pressDecoration(style, shape, interactionSource)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = rememberPressIndication(style),
                enabled = enabled,
                role = role,
                onLongClick = onLongClick,
                onClick = onClick
            )
    } else {
        this
            .pressDecoration(style, shape, interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberPressIndication(style),
                enabled = enabled,
                role = role,
                onClick = onClick
            )
    }
}

// ， onClick、 clickable  of （Surface / Button / combinedClickable），
// interactionSource
@Composable
fun Modifier.pressScale(
    style: PressStyle,
    interactionSource: InteractionSource
): Modifier = pressDecoration(style, RectangleShape, interactionSource)

// ， interactionSource、 indication  of  M3 （DropdownMenuItem ）。
@Composable
fun Modifier.pressHighlight(
    style: PressStyle,
    interactionSource: InteractionSource,
    shape: Shape = RectangleShape
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val alpha = remember { Animatable(0f) }
    val longPressDelayMs = LocalViewConfiguration.current.longPressTimeoutMillis
    LaunchedEffect(pressed, style.highlightAlpha) {
        if (pressed) {
            delay(longPressDelayMs)
            alpha.animateTo(style.highlightAlpha, PressDownSpec)
        } else {
            alpha.animateTo(0f, PressUpSpec)
        }
    }
    return if (style.highlightAlpha <= 0f) {
        this
    } else {
        clip(shape).drawWithContent {
            val current = alpha.value
            if (current > 0f) drawRect(color = Color.Black, alpha = current)
            drawContent()
        }
    }
}

@Composable
private fun Modifier.pressDecoration(
    style: PressStyle,
    shape: Shape,
    interactionSource: InteractionSource
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pressed, style.scale) {
        scale.animateTo(
            targetValue = if (pressed) style.scale else 1f,
            animationSpec = if (pressed) PressDownSpec else PressUpSpec
        )
    }
    val needsClip = shape !== RectangleShape
    return if (style.scale == 1f && !needsClip) {
        this
    } else {
        // layer  Animatable，，
        graphicsLayer {
            val current = scale.value
            scaleX = current
            scaleY = current
            this.shape = shape
            clip = needsClip
        }
    }
}

// pressable  graphicsLayer ， of  indication ，
@Composable
private fun rememberPressIndication(style: PressStyle): Indication? =
    remember(style.highlightAlpha) {
        if (style.highlightAlpha > 0f) {
            MimikyuPressIndication(pressedScale = 1f, dimAlpha = style.highlightAlpha)
        } else {
            null
        }
    }
