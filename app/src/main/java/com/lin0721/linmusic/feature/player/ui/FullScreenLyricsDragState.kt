package com.lin0721.linmusic.feature.player.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// of Close： and  of
class FullScreenLyricsDragState(
    private val scope: CoroutineScope,
    private val lazyListState: LazyListState,
    private val onClose: () -> Unit,
    private val onDragClose: () -> Unit = onClose,
    initialScreenHeightPx: Float = 0f
) {

    var offsetY by mutableFloatStateOf(0f)
        private set

    var viewportHeightPx by mutableFloatStateOf(0f)
        private set

    var screenHeightPx by mutableFloatStateOf(initialScreenHeightPx)
        private set

    // ， and Close
    private var isGestureStartedAtTop = true
    private var isScrollGestureActive = false
    private var dragReleaseJob: Job? = null

    fun reset() {
        dragReleaseJob?.cancel()
        offsetY = 0f
        isScrollGestureActive = false
        isGestureStartedAtTop = true
    }

    fun onViewportHeightChange(heightPx: Float) {
        viewportHeightPx = heightPx
    }

    fun onScreenHeightChange(heightPx: Float) {
        screenHeightPx = heightPx
    }

    // ，
    fun onHeaderDrag(delta: Float) {
        offsetY = (offsetY + delta).coerceAtLeast(0f)
    }

    fun onHeaderDragStart() {
        isGestureStartedAtTop = true
    }

    // Close
    fun handleDragRelease(velocity: Float = 0f) {
        dragReleaseJob?.cancel()
        dragReleaseJob = scope.launch {
            val totalHeight = if (screenHeightPx > 0f) screenHeightPx else 2400f
            val shouldClose = if (isGestureStartedAtTop) {
                offsetY > totalHeight * 0.10f || velocity > 450f
            } else {
                offsetY > totalHeight * 0.20f
            }

            if (offsetY > 0f && shouldClose) {
                animate(
                    initialValue = offsetY,
                    targetValue = totalHeight,
                    initialVelocity = velocity,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) { value, _ ->
                    offsetY = value
                }
                onDragClose()
            } else {
                animate(
                    initialValue = offsetY,
                    targetValue = 0f,
                    initialVelocity = velocity,
                    animationSpec = spring(
                        dampingRatio = 0.80f,
                        stiffness = 380f
                    )
                ) { value, _ ->
                    offsetY = value.coerceAtLeast(0f)
                }
            }
        }
    }

    // of  0.3 ，
    private fun damping(): Float = if (isGestureStartedAtTop) 1.0f else 0.3f

    private fun markGestureStart(source: NestedScrollSource) {
        if (source == NestedScrollSource.UserInput && !isScrollGestureActive) {
            isScrollGestureActive = true
            isGestureStartedAtTop = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
        }
    }

    val nestedScrollConnection = object : NestedScrollConnection {

        // ，，
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            markGestureStart(source)

            return if (offsetY > 0f && available.y < 0f) {
                val damping = damping()
                val delta = available.y * damping
                val consumed = delta.coerceAtLeast(-offsetY)
                offsetY += consumed
                Offset(0f, consumed / damping)
            } else {
                Offset.Zero
            }
        }

        // Close， and ；Close Header
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            markGestureStart(source)
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            isScrollGestureActive = false
            return if (offsetY > 0f) {
                handleDragRelease(velocity = available.y)
                available
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            isScrollGestureActive = false
            return if (offsetY > 0f) {
                handleDragRelease(velocity = available.y)
                available
            } else {
                Velocity.Zero
            }
        }
    }
}

// key  of  remember： and ，Create of  onClose，
// 。 key  and  of 。
@Composable
fun rememberFullScreenLyricsDragState(
    lazyListState: LazyListState,
    onClose: () -> Unit,
    onDragClose: () -> Unit = onClose,
    screenHeightPx: Float = 0f
): FullScreenLyricsDragState {
    val scope = rememberCoroutineScope()
    val state = remember {
        FullScreenLyricsDragState(
            scope = scope,
            lazyListState = lazyListState,
            onClose = onClose,
            onDragClose = onDragClose,
            initialScreenHeightPx = screenHeightPx
        )
    }
    if (screenHeightPx > 0f && state.screenHeightPx != screenHeightPx) {
        state.onScreenHeightChange(screenHeightPx)
    }
    return state
}
