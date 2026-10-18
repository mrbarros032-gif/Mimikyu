package com.lin0721.linmusic.feature.player.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.player.domain.LyricLine
import com.lin0721.linmusic.core.player.domain.LyricParser
import kotlinx.coroutines.isActive

private const val TAG = "KaraokeLyricRow"

// 逐字词的物理渲染坐标缓存，避免每帧重复调用 getBoundingBox 的 JNI 开销
private class WordLayout(
    val startMs: Long,
    val endMs: Long,
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
    val lineIndex: Int
)

private class LineLayout(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float
)

private class LyricLayoutInfo(
    val wordLayouts: List<WordLayout>,
    val lineLayouts: List<LineLayout>,
    val wordsByLine: List<List<WordLayout>>
)

// 基于高精度系统时钟的时钟插值器，将播放器底层轮询进度转换为 60/120fps 平滑进度
private class LyricTimeInterpolator {
    private var basePositionMs: Long = 0L
    private var anchorRealtimeNano: Long = 0L
    private var lastObservedRawPosition: Long = -1L

    fun getSmoothPosition(rawPosition: Long, isPlaying: Boolean): Long {
        val nowNano = SystemClock.elapsedRealtimeNanos()
        if (rawPosition != lastObservedRawPosition) {
            lastObservedRawPosition = rawPosition
            basePositionMs = rawPosition
            anchorRealtimeNano = nowNano
        }
        return if (isPlaying) {
            val elapsedMs = (nowNano - anchorRealtimeNano) / 1_000_000L
            val clampedElapsed = elapsedMs.coerceIn(0L, 500L)
            basePositionMs + clampedElapsed
        } else {
            basePositionMs
        }
    }
}

// 计算单行当前进度的擦除右边界，包含词内比例与词间过渡
private fun calculateLineProgressRight(
    lineIndex: Int,
    lineLayout: LineLayout,
    info: LyricLayoutInfo,
    relativeProgress: Long,
    lineDurationMs: Long = 0L
): Float {
    val wordsOnLine = info.wordsByLine.getOrElse(lineIndex) { emptyList() }
    val lastWordOnLine = wordsOnLine.lastOrNull()
    if (lastWordOnLine == null) {
        if (lineDurationMs > 0L) {
            val ratio = (relativeProgress.toFloat() / lineDurationMs).coerceIn(0f, 1f)
            return (lineLayout.left + (lineLayout.right - lineLayout.left) * ratio).coerceIn(lineLayout.left, lineLayout.right)
        }
        return lineLayout.left
    }
    if (relativeProgress >= lastWordOnLine.endMs) {
        return lineLayout.right
    }
    val firstWord = wordsOnLine.first()
    if (relativeProgress < firstWord.startMs) {
        return lineLayout.left
    }

    var maxRight = lineLayout.left
    for (i in wordsOnLine.indices) {
        val word = wordsOnLine[i]
        if (relativeProgress in word.startMs..word.endMs) {
            val wordDuration = (word.endMs - word.startMs).coerceAtLeast(1)
            val ratio = (relativeProgress - word.startMs).toFloat() / wordDuration
            val currentWordRight = word.left + (word.right - word.left) * ratio
            maxRight = maxRight.coerceAtLeast(currentWordRight)
            break
        } else if (relativeProgress > word.endMs) {
            maxRight = maxRight.coerceAtLeast(word.right)
            if (i + 1 < wordsOnLine.size) {
                val nextWord = wordsOnLine[i + 1]
                if (relativeProgress < nextWord.startMs) {
                    val gapDuration = (nextWord.startMs - word.endMs).coerceAtLeast(1)
                    if (gapDuration <= 300) {
                        val gapRatio = (relativeProgress - word.endMs).toFloat() / gapDuration
                        val bridgeX = word.right + (nextWord.left - word.right) * gapRatio
                        maxRight = maxRight.coerceAtLeast(bridgeX)
                    } else {
                        val bridgeTime = 60L
                        val gapElapsed = relativeProgress - word.endMs
                        if (gapElapsed < bridgeTime) {
                            val gapRatio = gapElapsed.toFloat() / bridgeTime
                            val bridgeX = word.right + (nextWord.left - word.right) * gapRatio
                            maxRight = maxRight.coerceAtLeast(bridgeX)
                        } else {
                            maxRight = maxRight.coerceAtLeast(nextWord.left)
                        }
                    }
                    break
                }
            }
        }
    }
    return maxRight.coerceIn(lineLayout.left, lineLayout.right)
}

// ────────────────────────────────────────────────────────────────────────────
// 逐字高亮（卡拉OK式）歌词行
// ────────────────────────────────────────────────────────────────────────────
@Composable
fun KaraokeLyricRow(
    line: LyricLine,
    currentPositionProvider: () -> Long,
    inactiveColor: Color,
    activeColor: Color,
    fontSize: TextUnit = 22.sp,
    lineHeight: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Start,
    advancedEffect: Boolean = true,
    isPlaying: Boolean = true
) {
    var textLayoutResult by remember(line) { mutableStateOf<TextLayoutResult?>(null) }
    val currentPositionProviderState = rememberUpdatedState(currentPositionProvider)
    val timeInterpolator = remember(line) { LyricTimeInterpolator() }
    var frameTick by remember(line) { mutableLongStateOf(0L) }

    LaunchedEffect(isPlaying, line) {
        if (isPlaying) {
            while (isActive) {
                withFrameNanos { frameNano ->
                    frameTick = frameNano
                }
            }
        }
    }

    val effectiveWords = remember(line) {
        if (line.words.isNotEmpty()) {
            line.words
        } else if (line.text.isNotBlank()) {
            val dur = if (line.durationMs > 0L) line.durationMs else LyricParser.estimateTextDurationMs(line.text)
            LyricParser.synthesizeWordTimings(line.copy(durationMs = dur))
        } else {
            emptyList()
        }
    }

    // 在排版结果解析后，仅计算并缓存一次每个字词与行的物理渲染坐标，彻底避免每帧重复调用 getBoundingBox 的 JNI 开销
    val lyricLayoutInfo = remember(line, effectiveWords, textLayoutResult) {
        val layout = textLayoutResult
        if (layout == null || effectiveWords.isEmpty()) null else {
            val textLength = line.text.length
            var currentSearchIndex = 0
            val wordRanges = effectiveWords.map { word ->
                val cleanW = word.text.trim()
                val startIndex = if (cleanW.isNotEmpty()) {
                    line.text.indexOf(cleanW, currentSearchIndex, ignoreCase = true).takeIf { it != -1 }
                        ?: line.text.indexOf(cleanW, 0, ignoreCase = true).takeIf { it != -1 }
                } else null

                if (startIndex != null) {
                    currentSearchIndex = (startIndex + cleanW.length).coerceAtMost(textLength)
                    startIndex until currentSearchIndex
                } else {
                    val start = currentSearchIndex.coerceAtMost(textLength)
                    currentSearchIndex = (currentSearchIndex + word.text.length).coerceAtMost(textLength)
                    start until currentSearchIndex
                }
            }

            val wordLayouts = effectiveWords.mapIndexed { i, word ->
                val range = wordRanges[i]
                val lineIndex = layout.getLineForOffset(range.first.coerceIn(0, textLength.coerceAtLeast(1) - 1))
                val lineTop = layout.getLineTop(lineIndex)
                val lineBottom = layout.getLineBottom(lineIndex)

                val wordLeft = try {
                    if (range.isEmpty()) layout.getHorizontalPosition(range.first.coerceIn(0, textLength), true)
                    else layout.getBoundingBox(range.first.coerceIn(0, (textLength - 1).coerceAtLeast(0))).left
                } catch (e: Exception) {
                    layout.getHorizontalPosition(range.first.coerceIn(0, textLength), true)
                }
                val wordRight = try {
                    if (range.isEmpty()) layout.getHorizontalPosition(range.first.coerceIn(0, textLength), true)
                    else layout.getBoundingBox(range.last.coerceIn(0, (textLength - 1).coerceAtLeast(0))).right
                } catch (e: Exception) {
                    layout.getHorizontalPosition((range.last + 1).coerceIn(0, textLength), true)
                }

                WordLayout(
                    startMs = word.startOffsetMs,
                    endMs = word.startOffsetMs + word.durationMs,
                    left = wordLeft,
                    right = wordRight,
                    top = lineTop,
                    bottom = lineBottom,
                    lineIndex = lineIndex
                )
            }

            val lineLayouts = (0 until layout.lineCount).map { lineIndex ->
                LineLayout(
                    left = layout.getLineLeft(lineIndex),
                    right = layout.getLineRight(lineIndex),
                    top = layout.getLineTop(lineIndex),
                    bottom = layout.getLineBottom(lineIndex)
                )
            }

            val wordsByLine = (0 until layout.lineCount).map { lineIndex ->
                wordLayouts.filter { it.lineIndex == lineIndex }
            }

            LyricLayoutInfo(wordLayouts, lineLayouts, wordsByLine)
        }
    }

    val boxAlignment = when (textAlign) {
        TextAlign.Center -> Alignment.Center
        TextAlign.End -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = boxAlignment
    ) {
        // 底层灰色（未激活）歌词
        Text(
            text = line.text,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = FontWeight.Bold,
            fontFamily = com.lin0721.linmusic.core.ui.theme.SFProFontFamily,
            letterSpacing = (-0.5).sp,
            color = inactiveColor,
            textAlign = textAlign,
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier.fillMaxWidth()
        )

        // 顶层高亮歌词
        if (advancedEffect) {
            Text(
                text = line.text,
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontWeight = FontWeight.Bold,
                fontFamily = com.lin0721.linmusic.core.ui.theme.SFProFontFamily,
                letterSpacing = (-0.5).sp,
                color = activeColor,
                textAlign = textAlign,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        val unused = frameTick
                        drawContent()
                        val info = lyricLayoutInfo
                        if (info == null) {
                            drawRect(color = Color.Black, blendMode = BlendMode.Clear)
                            return@drawWithContent
                        }
                        val smoothPosition = timeInterpolator.getSmoothPosition(
                            currentPositionProviderState.value(),
                            isPlaying
                        )
                        val relativeProgress = smoothPosition - line.timeMs
                        val featherPx = 18.dp.toPx()

                        // 逐行进行流光渐变推进遮罩擦除
                        info.lineLayouts.forEachIndexed { lineIndex, lineLayout ->
                            val maxRight = calculateLineProgressRight(lineIndex, lineLayout, info, relativeProgress, line.durationMs)

                            if (maxRight < lineLayout.right) {
                                val fadeStart = (maxRight - featherPx).coerceAtLeast(lineLayout.left)
                                val fadeEnd = maxRight.coerceAtMost(lineLayout.right)

                                if (fadeEnd < lineLayout.right) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(fadeEnd, lineLayout.top),
                                        size = Size(lineLayout.right - fadeEnd, lineLayout.bottom - lineLayout.top),
                                        blendMode = BlendMode.DstOut
                                    )
                                }
                                if (fadeEnd > fadeStart) {
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color.Transparent, Color.Black),
                                            startX = fadeStart,
                                            endX = fadeEnd
                                        ),
                                        topLeft = Offset(fadeStart, lineLayout.top),
                                        size = Size(fadeEnd - fadeStart, lineLayout.bottom - lineLayout.top),
                                        blendMode = BlendMode.DstOut
                                    )
                                }
                            }
                        }

                        // 当前字词呼吸光晕
                        info.wordLayouts.forEach { word ->
                            if (relativeProgress in word.startMs..word.endMs) {
                                val wordDuration = (word.endMs - word.startMs).coerceAtLeast(1)
                                val ratio = (relativeProgress - word.startMs).toFloat() / wordDuration
                                val pulse = kotlin.math.sin(ratio * kotlin.math.PI).toFloat()
                                if (pulse > 0f) {
                                    drawRect(
                                        color = Color.White.copy(alpha = 0.22f * pulse),
                                        topLeft = Offset(word.left, word.top),
                                        size = Size(word.right - word.left, word.bottom - word.top),
                                        blendMode = BlendMode.Plus
                                    )
                                }
                            }
                        }
                    }
            )
        } else {
            Text(
                text = line.text,
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontWeight = FontWeight.Bold,
                fontFamily = com.lin0721.linmusic.core.ui.theme.SFProFontFamily,
                letterSpacing = (-0.5).sp,
                color = activeColor,
                textAlign = textAlign,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val unused = frameTick
                        val info = lyricLayoutInfo
                        if (info == null) {
                            alpha = 0f
                        } else {
                            alpha = 1f
                            clip = true
                            shape = object : Shape {
                                override fun createOutline(
                                    size: Size,
                                    layoutDirection: LayoutDirection,
                                    density: Density
                                ): Outline {
                                    val path = androidx.compose.ui.graphics.Path()
                                    val smoothPosition = timeInterpolator.getSmoothPosition(
                                        currentPositionProviderState.value(),
                                        isPlaying
                                    )
                                    val relativeProgress = smoothPosition - line.timeMs

                                    info.lineLayouts.forEachIndexed { lineIndex, lineLayout ->
                                        val maxRight = calculateLineProgressRight(lineIndex, lineLayout, info, relativeProgress, line.durationMs)
                                        if (maxRight > lineLayout.left) {
                                            path.addRect(
                                                Rect(
                                                    left = lineLayout.left,
                                                    top = lineLayout.top,
                                                    right = maxRight,
                                                    bottom = lineLayout.bottom
                                                )
                                            )
                                        }
                                    }

                                    return Outline.Generic(path)
                                }
                            }
                        }
                    }
            )
        }
    }
}
