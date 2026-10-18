package com.lin0721.linmusic.core.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.ui.theme.SwipeCoverSpringSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ，，
private const val SwipeConfirmFraction = 0.32f
private const val SwipeConfirmVelocityPx = 1200f

// Previous/Next、， of
enum class SwipeDirection { NEXT, PREVIOUS }

// of （：Artists，
// +Artists）。containerWidthPx  of ，
// ，
@Stable
class SwipeToSkipCoverState {
    var offsetX by mutableFloatStateOf(0f)
        internal set
    var containerWidthPx by mutableFloatStateOf(0f)
    internal var settleJob: Job? = null
    internal var isDragActive by mutableStateOf(false)
    private var syncedKey: Any? = null
    private var lastPreviousKey: Any? = null
    private var lastNextKey: Any? = null

    private var isDragConfirmed = false

    var isTransitioning by mutableStateOf(false)
        internal set

    fun beginTransition() {
        isTransitioning = true
        isDragConfirmed = true
    }

    fun cancelTransition() {
        isTransitioning = false
        isDragConfirmed = false
        offsetX = 0f
    }

    fun markCancelTooLate() {
        isDragConfirmed = false
        isTransitioning = false
    }

    fun syncCurrentKey(key: Any?, previousKey: Any? = null, nextKey: Any? = null): SwipeDirection? {
        // Do not interrupt while the user is physically holding a finger down dragging
        if (isDragActive) return null
        var direction: SwipeDirection? = null
        if (syncedKey != key) {
            direction = when {
                isDragConfirmed -> null
                key == lastNextKey -> SwipeDirection.NEXT
                key == lastPreviousKey -> SwipeDirection.PREVIOUS
                else -> null
            }
            offsetX = when (direction) {
                SwipeDirection.NEXT -> containerWidthPx
                SwipeDirection.PREVIOUS -> -containerWidthPx
                null -> 0f
            }
            syncedKey = key
            isTransitioning = false
            isDragConfirmed = false
            lastPreviousKey = previousKey
            lastNextKey = nextKey
        } else {
            lastPreviousKey = previousKey
            lastNextKey = nextKey
            if (!isDragConfirmed && settleJob?.isActive != true) {
                isTransitioning = false
            }
        }
        return direction
    }
}

@Composable
fun rememberSwipeToSkipCoverState(): SwipeToSkipCoverState = remember { SwipeToSkipCoverState() }

// /，
private fun settleSwipe(
    state: SwipeToSkipCoverState,
    velocity: Float,
    canSwipeToPrevious: Boolean,
    canSwipeToNext: Boolean,
    onConfirmPrevious: () -> Unit,
    onConfirmNext: () -> Unit,
    onCancelPending: () -> Boolean,
    scope: CoroutineScope
) {
    val containerWidthPx = state.containerWidthPx
    val threshold = containerWidthPx * SwipeConfirmFraction
    val confirmNext = canSwipeToNext &&
        (state.offsetX < -threshold || velocity < -SwipeConfirmVelocityPx)
    val confirmPrevious = canSwipeToPrevious &&
        (state.offsetX > threshold || velocity > SwipeConfirmVelocityPx)
    // ， of ，Cancel
    val wasTransitioning = state.isTransitioning
    if (confirmNext || confirmPrevious) {
        state.beginTransition()
    }
    state.settleJob = scope.launch {
        when {
            confirmNext -> {
                animate(state.offsetX, -containerWidthPx, velocity, SwipeCoverSpringSpec) { value, _ ->
                    state.offsetX = value
                }
                onConfirmNext()
                // ， syncCurrentKey
            }
            confirmPrevious -> {
                animate(state.offsetX, containerWidthPx, velocity, SwipeCoverSpringSpec) { value, _ ->
                    state.offsetX = value
                }
                onConfirmPrevious()
            }
            else -> {
                animate(state.offsetX, 0f, velocity, SwipeCoverSpringSpec) { value, _ ->
                    state.offsetX = value
                }
                if (wasTransitioning) {
                    if (onCancelPending()) state.cancelTransition() else state.markCancelTooLate()
                }
            }
        }
    }
}

// 、 of  Modifier，
// of  state，（+Artists）
@Composable
fun rememberSwipeToSkipDragModifier(
    state: SwipeToSkipCoverState,
    canSwipeToPrevious: Boolean,
    canSwipeToNext: Boolean,
    onConfirmPrevious: () -> Unit,
    onConfirmNext: () -> Unit,
    onCancelPending: () -> Boolean = { false }
): Modifier {
    val scope = rememberCoroutineScope()
    if (!canSwipeToPrevious && !canSwipeToNext) return Modifier
    return Modifier.draggable(
        orientation = Orientation.Horizontal,
        state = rememberDraggableState { delta ->
            state.settleJob?.cancel()
            val maxNext = if (canSwipeToNext) state.containerWidthPx else 0f
            val maxPrevious = if (canSwipeToPrevious) state.containerWidthPx else 0f
            state.offsetX = (state.offsetX + delta).coerceIn(-maxNext, maxPrevious)
        },
        onDragStarted = { state.isDragActive = true },
        onDragStopped = { velocity ->
            state.isDragActive = false
            settleSwipe(state, velocity, canSwipeToPrevious, canSwipeToNext, onConfirmPrevious, onConfirmNext, onCancelPending, scope)
        }
    )
}

private data class OutgoingSlideInfo(
    val key: Any,
    val coverUrl: String,
    val direction: SwipeDirection
)

// ：Current/Previous/Next and
@Composable
fun SwipeToSkipCover(
    coverUrl: String,
    previousCoverUrl: String?,
    nextCoverUrl: String?,
    onConfirmPrevious: () -> Unit,
    onConfirmNext: () -> Unit,
    // Current of ：，
    // key  of （Back、currentTrack ）
    currentKey: Any,
    modifier: Modifier = Modifier,
    previousKey: Any? = null,
    nextKey: Any? = null,
    contentPadding: Dp = 0.dp,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RectangleShape,
    elevation: Dp = 0.dp,
    // of ；Back true
    onCancelPending: () -> Boolean = { false }
) {
    val scope = rememberCoroutineScope()
    var offsetX by remember { mutableFloatStateOf(0f) }
    var containerWidthPx by remember { mutableStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    // ，offsetX ， of  key ，
    // of ，/ of
    var isDragActive by remember { mutableStateOf(false) }
    var isGestureLocked by remember { mutableStateOf(false) }

    // currentKey ，Queue currentIndex
    // currentTrack ， of
    var isTransitioning by remember { mutableStateOf(false) }
    // of （）（/） of ，
    // of  of
    var isDragConfirmed by remember { mutableStateOf(false) }
    var displayedPreviousCoverUrl by remember { mutableStateOf(previousCoverUrl) }
    var displayedNextCoverUrl by remember { mutableStateOf(nextCoverUrl) }
    var displayedPreviousKey by remember { mutableStateOf(previousKey) }
    var displayedNextKey by remember { mutableStateOf(nextKey) }
    // Current of /Key ：，
    // coverUrl/currentKey "Current" of
    var displayedCoverUrl by remember { mutableStateOf(coverUrl) }
    var displayedCurrentKey by remember { mutableStateOf(currentKey) }
    var outgoingSlideInfo by remember { mutableStateOf<OutgoingSlideInfo?>(null) }
    if (!isTransitioning) {
        val currentKeyStr = currentKey.toString()
        val prevKeyStr = previousKey?.toString()
        val nextKeyStr = nextKey?.toString()
        // Previous/NextQueue， and CurrentKey，
        if ((prevKeyStr != null && prevKeyStr == currentKeyStr) ||
            (nextKeyStr != null && nextKeyStr == currentKeyStr)
        ) {
            isTransitioning = true
        } else {
            displayedPreviousCoverUrl = previousCoverUrl
            displayedNextCoverUrl = nextCoverUrl
            displayedPreviousKey = previousKey
            displayedNextKey = nextKey
            displayedCoverUrl = coverUrl
            displayedCurrentKey = currentKey
        }
    }
    val canSwipeToPrevious = displayedPreviousCoverUrl != null
    val canSwipeToNext = displayedNextCoverUrl != null

    var syncedKey by remember { mutableStateOf(currentKey) }
    var pendingSlideInKey by remember { mutableStateOf<Any?>(null) }
    if (!isDragActive && !isGestureLocked && settleJob?.isActive != true && syncedKey != currentKey) {
        val matchesNext = displayedNextKey != null && displayedNextKey.toString() == currentKey.toString()
        val matchesPrevious = displayedPreviousKey != null && displayedPreviousKey.toString() == currentKey.toString()
        val direction = if (isDragConfirmed) {
            null
        } else when {
            matchesNext -> SwipeDirection.NEXT
            matchesPrevious -> SwipeDirection.PREVIOUS
            else -> null
        }
        if (direction != null) {
            outgoingSlideInfo = OutgoingSlideInfo(
                key = displayedCurrentKey,
                coverUrl = displayedCoverUrl,
                direction = direction
            )
            offsetX = when (direction) {
                SwipeDirection.NEXT -> containerWidthPx
                SwipeDirection.PREVIOUS -> -containerWidthPx
            }
            pendingSlideInKey = currentKey
        } else {
            outgoingSlideInfo = null
            offsetX = 0f
        }
        displayedCoverUrl = coverUrl
        displayedCurrentKey = currentKey
        displayedPreviousCoverUrl = previousCoverUrl
        displayedNextCoverUrl = nextCoverUrl
        displayedPreviousKey = previousKey
        displayedNextKey = nextKey
        syncedKey = currentKey
        isTransitioning = false
        isDragConfirmed = false
    }
    LaunchedEffect(pendingSlideInKey) {
        if (pendingSlideInKey != null) {
            animate(offsetX, 0f, 0f, SwipeCoverSpringSpec) { value, _ ->
                offsetX = value
            }
            outgoingSlideInfo = null
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
            .then(
                if (containerWidthPx > 0f && (canSwipeToPrevious || canSwipeToNext)) {
                    Modifier.draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            settleJob?.cancel()
                            // Next/Previous
                            val maxNext = if (canSwipeToNext) containerWidthPx else 0f
                            val maxPrevious = if (canSwipeToPrevious) containerWidthPx else 0f
                            offsetX = (offsetX + delta).coerceIn(-maxNext, maxPrevious)
                        },
                        onDragStarted = {
                            isDragActive = true
                            isGestureLocked = true
                        },
                        onDragStopped = { velocity ->
                            isDragActive = false
                            val threshold = containerWidthPx * SwipeConfirmFraction
                            val confirmNext = canSwipeToNext &&
                                (offsetX < -threshold || velocity < -SwipeConfirmVelocityPx)
                            val confirmPrevious = canSwipeToPrevious &&
                                (offsetX > threshold || velocity > SwipeConfirmVelocityPx)
                            // ， of ，Cancel
                            val wasTransitioning = isTransitioning
                            if (confirmNext || confirmPrevious) {
                                isTransitioning = true
                                isDragConfirmed = true
                            }
                            settleJob = scope.launch {
                                try {
                                    when {
                                        confirmNext -> {
                                            animate(offsetX, -containerWidthPx, velocity, SwipeCoverSpringSpec) { value, _ ->
                                                offsetX = value
                                            }
                                            onConfirmNext()
                                        }
                                        confirmPrevious -> {
                                            animate(offsetX, containerWidthPx, velocity, SwipeCoverSpringSpec) { value, _ ->
                                                offsetX = value
                                            }
                                            onConfirmPrevious()
                                        }
                                        else -> {
                                            animate(offsetX, 0f, velocity, SwipeCoverSpringSpec) { value, _ ->
                                                offsetX = value
                                            }
                                            if (wasTransitioning) {
                                                if (onCancelPending()) {
                                                    isTransitioning = false
                                                    isDragConfirmed = false
                                                } else {
                                                    isDragConfirmed = false
                                                }
                                            }
                                        }
                                    }
                                } finally {
                                    isGestureLocked = false
                                }
                            }
                        }
                    )
                } else {
                    Modifier
                }
            )
    ) {
        val previewPreviousUrl = displayedPreviousCoverUrl
        val previewNextUrl = displayedNextCoverUrl
        val previewPrevKey = displayedPreviousKey
        val previewNxtKey = displayedNextKey
        val currentKeyStr = displayedCurrentKey.toString()

        val layers = buildList {
            val outgoing = outgoingSlideInfo
            if (outgoing != null) {
                when (outgoing.direction) {
                    SwipeDirection.NEXT -> {
                        add(SwipeCoverLayerSpec("${outgoing.key}-outgoing", outgoing.coverUrl) { offsetX - containerWidthPx })
                        add(SwipeCoverLayerSpec(displayedCurrentKey.toString(), displayedCoverUrl) { offsetX })
                        val nextUrl = displayedNextCoverUrl
                        if (nextUrl != null) {
                            val nextK = displayedNextKey?.toString() ?: "next"
                            add(SwipeCoverLayerSpec(nextK, nextUrl) { offsetX + containerWidthPx })
                        }
                    }
                    SwipeDirection.PREVIOUS -> {
                        val prevUrl = displayedPreviousCoverUrl
                        if (prevUrl != null) {
                            val prevK = displayedPreviousKey?.toString() ?: "prev"
                            add(SwipeCoverLayerSpec(prevK, prevUrl) { offsetX - containerWidthPx })
                        }
                        add(SwipeCoverLayerSpec(displayedCurrentKey.toString(), displayedCoverUrl) { offsetX })
                        add(SwipeCoverLayerSpec("${outgoing.key}-outgoing", outgoing.coverUrl) { offsetX + containerWidthPx })
                    }
                }
            } else {
                val previewPreviousUrl = displayedPreviousCoverUrl
                val previewNextUrl = displayedNextCoverUrl
                val previewPrevKey = displayedPreviousKey
                val previewNxtKey = displayedNextKey
                val currentKeyStr = displayedCurrentKey.toString()

                if (previewPreviousUrl != null) {
                    val prevKeyCalculated = when {
                        previewPrevKey != null -> {
                            val k = previewPrevKey.toString()
                            if (k == currentKeyStr) "$k-prev" else k
                        }
                        previewPreviousUrl == displayedCoverUrl -> "$previewPreviousUrl-prev"
                        else -> previewPreviousUrl
                    }
                    add(SwipeCoverLayerSpec(prevKeyCalculated, previewPreviousUrl) { offsetX - containerWidthPx })
                }
                add(SwipeCoverLayerSpec(currentKeyStr, displayedCoverUrl) { offsetX })
                if (previewNextUrl != null) {
                    val nextKeyCalculated = when {
                        previewNxtKey != null -> {
                            val k = previewNxtKey.toString()
                            val prevK = previewPrevKey?.toString()
                            if (k == prevK || k == currentKeyStr) "$k-next" else k
                        }
                        previewNextUrl == previewPreviousUrl || previewNextUrl == displayedCoverUrl -> "$previewNextUrl-next"
                        else -> previewNextUrl
                    }
                    add(SwipeCoverLayerSpec(nextKeyCalculated, previewNextUrl) { offsetX + containerWidthPx })
                }
            }
        }
        layers.forEach { layer ->
            key(layer.key) {
                SwipeCoverLayer(
                    url = layer.url,
                    contentScale = contentScale,
                    shape = shape,
                    elevation = elevation,
                    contentPadding = contentPadding,
                    translationXProvider = layer.translationXProvider
                )
            }
        }
    }
}

private class SwipeCoverLayerSpec(
    val key: Any,
    val url: String,
    val translationXProvider: () -> Float
) {
    constructor(url: String, translationXProvider: () -> Float) : this(url, url, translationXProvider)
}

@Composable
private fun SwipeCoverLayer(
    url: String,
    contentScale: ContentScale,
    shape: Shape,
    elevation: Dp,
    contentPadding: Dp,
    translationXProvider: () -> Float
) {
    var isTransparentCover by remember(url) {
        mutableStateOf(CoverContourShadowHelper.isTransparent(url) == true)
    }
    val scope = rememberCoroutineScope()

    AntiFlickerCoverImage(
        url = url,
        contentScale = contentScale,
        onImageLoaded = { bitmap ->
            scope.launch(Dispatchers.Default) {
                val isTrans = CoverContourShadowHelper.detectIsTransparentCover(bitmap)
                CoverContourShadowHelper.markTransparency(url, isTrans)
                if (isTrans != isTransparentCover) {
                    withContext(Dispatchers.Main) {
                        isTransparentCover = isTrans
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = translationXProvider() }
            .padding(horizontal = contentPadding)
            .aspectRatio(1f)
            .then(
                if (!isTransparentCover && elevation > 0.dp) {
                    Modifier.shadow(elevation = elevation, shape = shape, clip = false)
                } else {
                    Modifier
                }
            )
            .then(
                if (!isTransparentCover) {
                    Modifier.clip(shape)
                } else {
                    Modifier
                }
            )
    )
}
