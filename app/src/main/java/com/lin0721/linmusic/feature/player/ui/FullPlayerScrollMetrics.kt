package com.lin0721.linmusic.feature.player.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

// of ：、、
@Stable
class FullPlayerScrollMetrics(private val listState: LazyListState) {

    // ， of
    private var coverHeight by mutableStateOf(1000f)

    val showTitleInBar by derivedStateOf { listState.firstVisibleItemIndex > 0 }

    val backgroundTranslationY by derivedStateOf {
        val visibleItems = listState.layoutInfo.visibleItemsInfo
        val coverItem = visibleItems.firstOrNull { it.key == "cover" }
        if (coverItem != null) {
            coverHeight = coverItem.size.toFloat()
            coverItem.offset.toFloat()
        } else {
            val firstIndex = listState.firstVisibleItemIndex
            val firstOffset = listState.firstVisibleItemScrollOffset
            val estimatedSubsequentScroll = (firstIndex - 1) * 250f + firstOffset
            -(coverHeight + estimatedSubsequentScroll)
        }
    }
}

@Composable
fun rememberFullPlayerScrollMetrics(listState: LazyListState): FullPlayerScrollMetrics =
    remember(listState) { FullPlayerScrollMetrics(listState) }

const val FullPlayerFillGapKeyPrefix = "fill_gap_"

// All，，Play， of ；
// contentKeys  of ，
@Composable
fun rememberFullPlayerFillGap(
    listState: LazyListState,
    enabled: Boolean,
    gapCount: Int,
    contentKeys: List<String>,
    bottomReserve: Dp
): Dp {
    val density = LocalDensity.current
    val bottomReservePx = with(density) { bottomReserve.roundToPx() }
    // ， of ，
    val cachedSizes = remember(listState) { HashMap<String, Int>() }
    val gapPx by remember(listState, enabled, gapCount, contentKeys, bottomReservePx) {
        derivedStateOf {
            if (!enabled || gapCount <= 0) return@derivedStateOf 0
            val info = listState.layoutInfo
            info.visibleItemsInfo.forEach { item ->
                val key = item.key as? String ?: return@forEach
                if (key in contentKeys) cachedSizes[key] = item.size
            }
            // ，
            if (contentKeys.any { it !in cachedSizes }) return@derivedStateOf 0
            val available = info.viewportSize.height - info.beforeContentPadding - bottomReservePx
            val remaining = available - contentKeys.sumOf { cachedSizes.getValue(it) }
            (remaining / gapCount).coerceAtLeast(0)
        }
    }
    return with(density) { gapPx.toDp() }
}
