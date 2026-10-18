package com.lin0721.linmusic.feature.player.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

// of ，key  and  fullPlayerPlaybackSection  of
private const val FIT_ANCHOR_KEY = "actions"
private val CoverMinEdge = 160.dp

// ，Play
private var lastSettledInsetPx = 0f

// ：、， of
private const val SETTLE_DELAY_MS = 250L

// of ， of
private val FitReserve = 12.dp

// FitReserve + FitHysteresis ，，
private val FitHysteresis = 24.dp

// ， of ，
private val AnchorProbeStep = 48.dp

// （ bottomReservePx） of （ 1px， 2px）。
// 、，Current
@Composable
fun rememberCoverFitInsetPx(
    listState: LazyListState,
    enabled: Boolean,
    bottomReservePx: Float = 0f
): Float {
    var insetPx by remember { mutableFloatStateOf(lastSettledInsetPx) }
    if (!enabled) return 0f

    val density = LocalDensity.current
    LaunchedEffect(listState, density, bottomReservePx) {
        val minEdgePx = with(density) { CoverMinEdge.toPx() }
        val defaultPaddingPx = with(density) { MimikyuSpacing.lg.toPx() }
        val reservePx = with(density) { FitReserve.toPx() }
        val hysteresisPx = with(density) { FitHysteresis.toPx() }
        val probeStepPx = with(density) { AnchorProbeStep.toPx() }
        snapshotFlow { listState.layoutInfo }.collectLatest {
            delay(SETTLE_DELAY_MS)
            if (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0) return@collectLatest
            val info = listState.layoutInfo
            val items = info.visibleItemsInfo
            if (items.isEmpty()) return@collectLatest

            val anchor = items.firstOrNull { it.key == FIT_ANCHOR_KEY }
            val fitEndPx = info.viewportEndOffset - bottomReservePx.coerceAtLeast(0f)
            // of ， of
            val heightDeltaPx = if (anchor != null) {
                val overflowPx = anchor.offset + anchor.size - fitEndPx
                when {
                    overflowPx > 0f -> overflowPx + reservePx
                    -overflowPx > reservePx + hysteresisPx -> overflowPx + reservePx
                    else -> 0f
                }
            } else {
                val last = items.last()
                (last.offset + last.size - fitEndPx).coerceAtLeast(0f) + probeStepPx
            }
            if (heightDeltaPx == 0f) return@collectLatest

            val maxInsetPx = ((info.viewportSize.width - defaultPaddingPx * 2 - minEdgePx) / 2f).coerceAtLeast(0f)
            val next = (insetPx + heightDeltaPx / 2f).coerceIn(0f, maxInsetPx)
            if (next != insetPx) {
                insetPx = next
                lastSettledInsetPx = next
            }
        }
    }
    return insetPx
}
