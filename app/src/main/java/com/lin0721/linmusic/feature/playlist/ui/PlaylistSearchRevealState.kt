package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// ────────────────────────────────────────────────────────────────────────────
// SearchExpand： + fling 。
// / delta  of （、fling
// 、 of ）， revealPx ，
// ，。 FullPlayerDragNestedScroll  of  onPre/onPostScroll 。
// ────────────────────────────────────────────────────────────────────────────
class SearchBarRevealState internal constructor(
    private val listState: LazyListState,
    private val isDailyRecommend: Boolean,
    private val scope: CoroutineScope,
    private val flingVelocityThresholdPx: Float
) {
    // Search， onSizeChanged
    var searchBarHeightPx by mutableFloatStateOf(0f)

    // 0f ，searchBarHeightPx Expand
    var revealPx by mutableFloatStateOf(0f)
        private set

    // Search of ：Confirm， of  revealPx
    var isOpen by mutableStateOf(false)
        private set

    private var settleJob: Job? = null

    // ：Expand， of  of ，
    private fun applyDampedOpen(delta: Float) {
        val max = searchBarHeightPx
        if (max <= 0f) return
        val remaining = (max - revealPx).coerceAtLeast(0f)
        val dampingFactor = (remaining / max).coerceIn(0f, 1f)
        revealPx = (revealPx + delta * dampingFactor).coerceIn(0f, max)
    }

    // Collapse：，，Expand 0
    private fun applyClose(delta: Float) {
        val max = searchBarHeightPx
        if (max <= 0f) return
        revealPx = (revealPx + delta).coerceIn(0f, max)
    }

    val connection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (isDailyRecommend || isOpen || revealPx <= 0f || available.y >= 0f) return Offset.Zero
            settleJob?.cancel()
            applyClose(available.y)
            return available
        }

        // 、 of ，SearchExpand
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (isDailyRecommend || isOpen || source != NestedScrollSource.UserInput || available.y <= 0f) {
                return Offset.Zero
            }
            val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
            if (!atTop) return Offset.Zero
            settleJob?.cancel()
            applyDampedOpen(available.y)
            return Offset(0f, available.y)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (isDailyRecommend || isOpen || revealPx <= 0f) return Velocity.Zero
            settle(available.y)
            return available
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (isDailyRecommend || isOpen || revealPx <= 0f) return Velocity.Zero
            settle(available.y)
            return available
        }
    }

    // Open Search bar programmatically
    fun open() {
        val max = if (searchBarHeightPx > 0f) searchBarHeightPx else 150f
        isOpen = true
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(
                initialValue = revealPx,
                targetValue = max,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) { value, _ ->
                revealPx = value.coerceIn(0f, max)
            }
        }
    }

    // SearchBack：Expand of
    fun close() {
        val max = searchBarHeightPx
        if (max <= 0f || !isOpen) return
        isOpen = false
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(
                initialValue = revealPx,
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) { value, _ ->
                revealPx = value.coerceIn(0f, max)
            }
        }
    }

    // ：， of
    private fun settle(velocity: Float) {
        val max = searchBarHeightPx
        if (max <= 0f) return
        val target = when {
            velocity > flingVelocityThresholdPx -> max
            velocity < -flingVelocityThresholdPx -> 0f
            revealPx > max / 2f -> max
            else -> 0f
        }
        isOpen = target > 0f
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(
                initialValue = revealPx,
                targetValue = target,
                initialVelocity = velocity,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) { value, _ ->
                revealPx = value.coerceIn(0f, max)
            }
        }
    }
}

@Composable
fun rememberSearchBarRevealState(
    listState: LazyListState,
    isDailyRecommend: Boolean
): SearchBarRevealState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val flingVelocityThresholdPx = with(density) { 300.dp.toPx() }
    return remember(listState, isDailyRecommend) {
        SearchBarRevealState(
            listState = listState,
            isDailyRecommend = isDailyRecommend,
            scope = scope,
            flingVelocityThresholdPx = flingVelocityThresholdPx
        )
    }
}
