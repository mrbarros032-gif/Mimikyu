package com.lin0721.linmusic.core.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// /（Play of ）。
// statusBarsPadding  consumeWindowInsets ， WindowInsets  of
val LocalMimikyuSystemBarsConsumed = compositionLocalOf { false }

@Composable
fun mimikyuStatusBarTopPadding(): Dp =
    if (LocalMimikyuSystemBarsConsumed.current) 0.dp
    else WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

@Composable
fun mimikyuNavigationBarBottomPadding(): Dp =
    if (LocalMimikyuSystemBarsConsumed.current) 0.dp
    else WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
