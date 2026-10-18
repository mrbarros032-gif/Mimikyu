package com.lin0721.linmusic.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.ui.theme.LocalMimikyuWindowSizeClass
import com.lin0721.linmusic.core.ui.theme.MimikyuWindowSizeClass

// Expanded /，；
// Compact  fillMaxWidth()，
private val DefaultMaxContentWidth = 680.dp

@Composable
fun AdaptiveContentWidth(
    modifier: Modifier = Modifier,
    maxWidth: Dp = DefaultMaxContentWidth,
    content: @Composable BoxScope.() -> Unit
) {
    val windowSizeClass = LocalMimikyuWindowSizeClass.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = if (windowSizeClass == MimikyuWindowSizeClass.Expanded) {
                Modifier.widthIn(max = maxWidth).fillMaxHeight()
            } else {
                Modifier.fillMaxWidth().fillMaxHeight()
            },
            content = content
        )
    }
}
