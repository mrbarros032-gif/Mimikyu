package com.lin0721.linmusic

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class AppSidebarState {
    Closed, Open
}

// ：、 and Expand
@OptIn(ExperimentalFoundationApi::class)
class MimikyuSidebarState(
    private val scope: CoroutineScope,
    val widthPx: Float,
    val draggableState: AnchoredDraggableState<AppSidebarState>
) {

    // ： and
    var isTouchStartingAtEdge by mutableStateOf(false)
        private set

    val isOpen: Boolean get() = draggableState.currentValue == AppSidebarState.Open

    // 0f Collapse，1f Expand； offset  NaN
    val progress: Float
        get() {
            val offset = draggableState.offset
            return if (offset.isNaN()) 0f else (offset / widthPx).coerceIn(0f, 1f)
        }

    val offsetX: Float
        get() = draggableState.offset.let { if (it.isNaN()) 0f else it }

    // Close；Close
    fun onPointerDown(positionX: Float, edgeWidthPx: Float) {
        isTouchStartingAtEdge = isOpen || positionX < edgeWidthPx
    }

    fun onPointerUp() {
        isTouchStartingAtEdge = false
    }

    fun open() {
        scope.launch { draggableState.animateTo(AppSidebarState.Open) }
    }

    fun close() {
        scope.launch { draggableState.animateTo(AppSidebarState.Closed) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberMimikyuSidebarState(width: androidx.compose.ui.unit.Dp = 310.dp): MimikyuSidebarState {
    val scope = rememberCoroutineScope()
    val density: Density = LocalDensity.current
    val widthPx = with(density) { width.toPx() }

    val draggableState = remember {
        AnchoredDraggableState(
            initialValue = AppSidebarState.Closed,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            decayAnimationSpec = exponentialDecay()
        )
    }

    LaunchedEffect(widthPx) {
        draggableState.updateAnchors(
            DraggableAnchors {
                AppSidebarState.Closed at 0f
                AppSidebarState.Open at widthPx
            }
        )
    }

    return remember(scope, widthPx, draggableState) {
        MimikyuSidebarState(scope, widthPx, draggableState)
    }
}
