package com.lin0721.linmusic.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.core.player.domain.LyricLine
import com.lin0721.linmusic.core.preferences.SettingsPreferences
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.PillRadius
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val LeftColumnMaxWidth = 420.dp
private val LeftColumnStartPadding = 48.dp
private val ColumnGap = 56.dp
private val RightColumnEndPadding = 24.dp
private val NoLyricsHeight = 56.dp

// Current， and
private const val LYRICS_RESUME_DELAY_MS = 5000L

private const val WIDE_LYRICS_KEY = "wide_lyrics"
private const val WIDE_BOTTOM_SPACER_KEY = "wide_bottom_spacer"

// Play： and Play；，，。
// ，
@Composable
fun FullPlayerWideLayout(
    sourceBar: @Composable (Modifier) -> Unit,
    cover: @Composable (Modifier) -> Unit,
    playbackControls: @Composable ColumnScope.() -> Unit,
    lyrics: List<LyricLine>,
    isLyricsLoading: Boolean,
    currentLyricIndex: Int,
    highlightColor: Color,
    currentPositionProvider: () -> Long,
    isPlaying: Boolean,
    onLyricClick: (LyricLine) -> Unit,
    onLyricsVisibleChange: (Boolean) -> Unit,
    infoCards: LazyListScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        sourceBar(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MimikyuSpacing.lg)
                .padding(top = MimikyuSpacing.md)
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = MimikyuSpacing.sm, bottom = MimikyuSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .padding(start = LeftColumnStartPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = LeftColumnMaxWidth)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    // and ，
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        cover(Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true))
                    }
                    Spacer(modifier = Modifier.height(MimikyuSpacing.md))
                    playbackControls()
                }
            }
            Spacer(modifier = Modifier.width(ColumnGap))
            WideRightColumn(
                lyrics = lyrics,
                isLyricsLoading = isLyricsLoading,
                currentLyricIndex = currentLyricIndex,
                highlightColor = highlightColor,
                currentPositionProvider = currentPositionProvider,
                isPlaying = isPlaying,
                onLyricClick = onLyricClick,
                onLyricsVisibleChange = onLyricsVisibleChange,
                infoCards = infoCards,
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .padding(end = RightColumnEndPadding)
            )
        }
    }
}

@Composable
private fun WideRightColumn(
    lyrics: List<LyricLine>,
    isLyricsLoading: Boolean,
    currentLyricIndex: Int,
    highlightColor: Color,
    currentPositionProvider: () -> Long,
    isPlaying: Boolean,
    onLyricClick: (LyricLine) -> Unit,
    onLyricsVisibleChange: (Boolean) -> Unit,
    infoCards: LazyListScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    val settingsPreferences: SettingsPreferences = koinInject()
    val lyricTextSize by settingsPreferences.fullScreenLyricTextSize.collectAsStateWithLifecycle(initialValue = 34)
    val lyricAlignment by settingsPreferences.fullScreenLyricAlignment.collectAsStateWithLifecycle(initialValue = "left")
    val lyricSecondaryMode by settingsPreferences.fullScreenLyricSecondaryMode.collectAsStateWithLifecycle(initialValue = "translation")
    val karaokeAdvancedEffect by settingsPreferences.fullScreenKaraokeAdvancedEffect.collectAsStateWithLifecycle(initialValue = true)
    val lyricLineSpacing by settingsPreferences.fullScreenLyricLineSpacing.collectAsStateWithLifecycle(initialValue = 24)
    val lyricSecondarySpacing by settingsPreferences.fullScreenLyricSecondarySpacing.collectAsStateWithLifecycle(initialValue = 6)

    val scope = rememberCoroutineScope()
    val columnState = rememberLazyListState()
    val lyricsListState = rememberLazyListState()
    val lyricsFling = ScrollableDefaults.flingBehavior()
    var lyricsViewportPx by remember { mutableFloatStateOf(0f) }
    var isUserScrollingLyrics by remember { mutableStateOf(false) }
    var resumeJob by remember { mutableStateOf<Job?>(null) }
    val isPlayingState = rememberUpdatedState(isPlaying)

    // of （），
    val lineTextBounds = remember { HashMap<Int, Rect>() }
    var lyricsOriginInRoot by remember { mutableStateOf(Offset.Zero) }

    val isPureMusic = lyrics.size == 1 && lyrics[0].text == "Instrumental"
    val hasLyrics = lyrics.isNotEmpty() && !isPureMusic

    LaunchedEffect(isPlaying) {
        if (isPlaying && isUserScrollingLyrics) isUserScrollingLyrics = false
        if (!isPlaying) resumeJob?.cancel()
    }

    // of ，PlayCollapse；Collapse
    val blockDragToClose = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
        }
    }

    val showBackToLyrics by remember(hasLyrics) {
        derivedStateOf { hasLyrics && columnState.firstVisibleItemIndex > 0 }
    }

    Box(modifier = modifier.nestedScroll(blockDragToClose)) {
        // contentPadding ：fillParentMaxHeight ，
        LazyColumn(
            state = columnState,
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = WIDE_LYRICS_KEY) {
                DisposableEffect(Unit) {
                    onLyricsVisibleChange(true)
                    onDispose { onLyricsVisibleChange(false) }
                }
                when {
                    isLyricsLoading || hasLyrics -> Column(
                        modifier = Modifier
                            .fillParentMaxHeight()
                            .fillMaxWidth()
                            .onGloballyPositioned { lyricsOriginInRoot = it.positionInRoot() }
                            .pointerInput(lyricsListState) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val downInRoot = down.position + lyricsOriginInRoot
                                    if (lineTextBounds.values.none { it.contains(downInRoot) }) return@awaitEachGesture

                                    val velocityTracker = VelocityTracker()
                                    velocityTracker.addPointerInputChange(down)
                                    var slopOvershoot = 0f
                                    val drag = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
                                        change.consume()
                                        slopOvershoot = overSlop
                                    } ?: return@awaitEachGesture

                                    resumeJob?.cancel()
                                    isUserScrollingLyrics = true
                                    lyricsListState.dispatchRawDelta(-slopOvershoot)
                                    verticalDrag(drag.id) { change ->
                                        velocityTracker.addPointerInputChange(change)
                                        lyricsListState.dispatchRawDelta(-change.positionChange().y)
                                        change.consume()
                                    }
                                    val velocityY = velocityTracker.calculateVelocity().y
                                    scope.launch {
                                        lyricsListState.scroll {
                                            with(lyricsFling) { performFling(-velocityY) }
                                        }
                                    }
                                    if (isPlayingState.value) {
                                        resumeJob = scope.launch {
                                            delay(LYRICS_RESUME_DELAY_MS)
                                            isUserScrollingLyrics = false
                                        }
                                    }
                                }
                            }
                    ) {
                        FullScreenLyricsList(
                            lyrics = lyrics,
                            currentIndex = currentLyricIndex,
                            isLoading = isLyricsLoading,
                            isUserScrolling = isUserScrollingLyrics,
                            highlightColor = highlightColor,
                            currentPositionProvider = currentPositionProvider,
                            lazyListState = lyricsListState,
                            viewportHeightPx = lyricsViewportPx,
                            onViewportHeightChange = { lyricsViewportPx = it },
                            gestureModifier = Modifier,
                            fontSize = lyricTextSize,
                            alignment = lyricAlignment,
                            secondaryMode = lyricSecondaryMode,
                            lineSpacing = lyricLineSpacing,
                            secondarySpacing = lyricSecondarySpacing,
                            advancedKaraokeEffect = karaokeAdvancedEffect,
                            isPlaying = isPlaying,
                            showSeekGuide = false,
                            userScrollEnabled = false,
                            onLineTextBounds = { index, bounds -> lineTextBounds[index] = bounds },
                            onSeek = {},
                            onLyricClick = { line ->
                                resumeJob?.cancel()
                                isUserScrollingLyrics = false
                                onLyricClick(line)
                            }
                        )
                    }
                    else -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(NoLyricsHeight)
                            .padding(horizontal = MimikyuSpacing.md),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = if (isPureMusic) "Instrumental, enjoy the music" else "No lyrics available",
                            color = highlightColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            infoCards()
            item(key = WIDE_BOTTOM_SPACER_KEY) {
                Spacer(modifier = Modifier.height(MimikyuSpacing.lg))
            }
        }

        AnimatedVisibility(
            visible = showBackToLyrics,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = MimikyuSpacing.sm)
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(PillRadius),
                modifier = Modifier.pressable(MimikyuPress.Pill) {
                    scope.launch { columnState.animateScrollToItem(0) }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = MimikyuSpacing.md, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(MimikyuSpacing.xs))
                    Text(text = "Back to lyrics", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
