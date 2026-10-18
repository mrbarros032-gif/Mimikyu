package com.lin0721.linmusic.feature.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.lyrics.toAccompanistSyncedLyrics
import com.lin0721.linmusic.core.player.domain.LyricLine
import com.lin0721.linmusic.core.ui.theme.SFProFontFamily
import com.mocharealm.accompanist.lyrics.ui.composable.list.rememberLyricsLazyListState
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.LyricsAnchor
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.LyricsFade

/**
 * Accompanist-powered Apple Music-style lyrics view for full screen player.
 * Renders high-performance syllable-by-syllable karaoke fluid glow,
 * natural breathing dots for interludes, and gesture-driven auto-scrolling.
 */
@Composable
fun ColumnScope.FullScreenLyricsList(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    isLoading: Boolean,
    isUserScrolling: Boolean,
    highlightColor: Color,
    currentPositionProvider: () -> Long,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    viewportHeightPx: Float,
    onViewportHeightChange: (Float) -> Unit,
    gestureModifier: Modifier,
    fontSize: Int = 34,
    alignment: String = "left",
    secondaryMode: String = "translation",
    lineSpacing: Int = 24,
    secondarySpacing: Int = 6,
    advancedKaraokeEffect: Boolean = true,
    isPlaying: Boolean = true,
    showSeekGuide: Boolean = true,
    userScrollEnabled: Boolean = true,
    onLineTextBounds: ((index: Int, bounds: Rect) -> Unit)? = null,
    lyricOffsetMs: Long = 0L,
    onSeek: (Long) -> Unit,
    onLyricClick: (LyricLine) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(gestureModifier),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = highlightColor)
        }
        return
    }

    if (lyrics.isEmpty()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(gestureModifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No lyrics available",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = SFProFontFamily
            )
        }
        return
    }

    val accompanistLyrics = remember(lyrics) {
        lyrics.toAccompanistSyncedLyrics()
    }
    val lyricsListState = rememberLyricsLazyListState()

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .then(gestureModifier)
    ) {
        KaraokeLyricsView(
            listState = lyricsListState,
            lyrics = accompanistLyrics,
            currentPosition = { currentPositionProvider().toInt() },
            onLineClicked = { line ->
                onSeek(line.start.toLong())
            },
            onLinePressed = { line ->
                onSeek(line.start.toLong())
            },
            modifier = Modifier.fillMaxSize(),
            normalLineTextStyle = LocalTextStyle.current.copy(
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.35f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SFProFontFamily,
                letterSpacing = (-0.5).sp
            ),
            translationTextStyle = LocalTextStyle.current.copy(
                fontSize = (fontSize * 0.55f).coerceAtLeast(14f).sp,
                lineHeight = (fontSize * 0.75f).coerceAtLeast(18f).sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SFProFontFamily
            ),
            accompanimentLineTextStyle = LocalTextStyle.current.copy(
                fontSize = (fontSize * 0.75f).coerceAtLeast(18f).sp,
                lineHeight = (fontSize * 1.05f).coerceAtLeast(22f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SFProFontFamily
            ),
            textColor = Color.White,
            itemSpacing = lineSpacing.dp,
            showTranslation = (secondaryMode == "translation"),
            showPhonetic = (secondaryMode == "roma"),
            anchor = LyricsAnchor.Fraction(0.38f),
            topFade = LyricsFade.ToAnchor(20.dp),
            bottomFade = LyricsFade.Fraction(0.45f)
        )
    }
}
