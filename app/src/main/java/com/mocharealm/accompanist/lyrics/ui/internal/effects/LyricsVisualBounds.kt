package com.mocharealm.accompanist.lyrics.ui.internal.effects

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.layout

// Alignment lines propagate through Columns/Boxes with their placement offsets. They describe
// retained drawing space, independently of the shrinking height consumed by list layout.
internal val LyricsVisualTop = HorizontalAlignmentLine(::minOf)
internal val LyricsVisualBottom = HorizontalAlignmentLine(::maxOf)

internal fun Modifier.lyricsVisualLayer(
    observePaint: () -> Unit,
    block: GraphicsLayerScope.() -> Unit,
): Modifier =
    lyricsLayerPaintObserver(observePaint).layout { measurable, constraints ->
        val child = measurable.measure(constraints)
        layout(child.width, child.height) {
            child.placeWithLayer(0, 0) {
                block()
                clip = false
            }
        }
    }
