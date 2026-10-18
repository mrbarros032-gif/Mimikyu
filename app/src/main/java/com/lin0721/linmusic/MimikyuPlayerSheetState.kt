package com.lin0721.linmusic

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Play of Expand/Collapse： and
@Stable
class MimikyuPlayerSheetState(private val scope: CoroutineScope) {

    var isOpen by mutableStateOf(false)
        private set

    // ，Play
    var offsetY by mutableStateOf(10000f)
        private set

    var screenHeightPx by mutableStateOf(0f)
        private set

    // mutableStateOf， of  recomposition
    private val springJobRef = arrayOfNulls<Job>(1)

    fun onScreenSizeChanged(heightPx: Float) {
        screenHeightPx = heightPx
    }

    // Play
    internal fun settleToBottomIfClosed() {
        if (screenHeightPx > 0f && !isOpen) {
            offsetY = screenHeightPx
        }
    }

    fun animateTo(open: Boolean, velocity: Float, initialOffset: Float = Float.NaN) {
        springJobRef[0]?.cancel()
        if (open) {
            isOpen = true
            springJobRef[0] = scope.launch {
                animate(
                    initialValue = offsetY,
                    targetValue = 0f,
                    initialVelocity = velocity,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) { value, _ -> offsetY = value.coerceIn(0f, screenHeightPx) }
            }
        } else {
            if (!initialOffset.isNaN()) {
                offsetY = initialOffset
            }
            springJobRef[0] = scope.launch {
                animate(
                    initialValue = offsetY,
                    targetValue = screenHeightPx,
                    initialVelocity = velocity,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) { value, _ -> offsetY = value.coerceIn(0f, screenHeightPx) }
                offsetY = screenHeightPx
                isOpen = false
            }
        }
    }

    // ：Cancel of ，
    fun onDrag(delta: Float) {
        springJobRef[0]?.cancel()
        if (!isOpen) isOpen = true
        offsetY = (offsetY + delta).coerceIn(0f, screenHeightPx)
    }

    // Expand
    fun onDragEnd(velocity: Float) {
        val shouldOpen = offsetY < screenHeightPx * 0.80f || velocity < -1000f
        animateTo(shouldOpen, velocity)
    }
}

@Composable
fun rememberMimikyuPlayerSheetState(): MimikyuPlayerSheetState {
    val scope = rememberCoroutineScope()
    val state = remember(scope) { MimikyuPlayerSheetState(scope) }
    LaunchedEffect(state.screenHeightPx) {
        state.settleToBottomIfClosed()
    }
    return state
}
