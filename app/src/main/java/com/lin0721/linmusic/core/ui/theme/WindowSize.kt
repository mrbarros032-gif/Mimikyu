package com.lin0721.linmusic.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ：（ 4 ）
enum class MimikyuWindowSizeClass {
    Compact,
    Expanded
}

private const val EXPANDED_MIN_WIDTH_DP = 600

@Composable
fun rememberMimikyuWindowSizeClass(): MimikyuWindowSizeClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return remember(widthDp) {
        if (widthDp >= EXPANDED_MIN_WIDTH_DP) {
            MimikyuWindowSizeClass.Expanded
        } else {
            MimikyuWindowSizeClass.Compact
        }
    }
}

// Play， compositionLocalOf  of ，
val LocalMimikyuWindowSizeClass = compositionLocalOf { MimikyuWindowSizeClass.Compact }

// ， and 。、，
// （、MV Play+） Expanded  of
enum class MimikyuOrientationClass {
    Portrait,
    Landscape
}

@Composable
fun rememberMimikyuOrientationClass(): MimikyuOrientationClass {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        if (configuration.screenWidthDp >= configuration.screenHeightDp) {
            MimikyuOrientationClass.Landscape
        } else {
            MimikyuOrientationClass.Portrait
        }
    }
}

val LocalMimikyuOrientationClass = compositionLocalOf { MimikyuOrientationClass.Portrait }

// of ，
// compact/expandedPortrait/expandedLandscape ， when
@Composable
fun rememberMimikyuGridColumns(
    compact: Int,
    expandedPortrait: Int,
    expandedLandscape: Int = expandedPortrait
): Int {
    val windowSizeClass = LocalMimikyuWindowSizeClass.current
    val orientationClass = LocalMimikyuOrientationClass.current
    return when {
        windowSizeClass == MimikyuWindowSizeClass.Expanded && orientationClass == MimikyuOrientationClass.Landscape -> expandedLandscape
        windowSizeClass == MimikyuWindowSizeClass.Expanded -> expandedPortrait
        else -> compact
    }
}

// Play： 380dp /  440dp。
// Collapse of Play and Expand of ，
private val PlayerPanelWidthPortrait = 380.dp
private val PlayerPanelWidthLandscape = 440.dp

@Composable
fun rememberMimikyuPlayerPanelWidth(): Dp {
    return if (rememberMimikyuOrientationClass() == MimikyuOrientationClass.Landscape) {
        PlayerPanelWidthLandscape
    } else {
        PlayerPanelWidthPortrait
    }
}

// Play，。
// Expanded ：， of 。
// Provider，
@Composable
fun ProvideMimikyuContentSizeClass(
    reservedWidth: Dp,
    content: @Composable () -> Unit
) {
    val windowSizeClass = LocalMimikyuWindowSizeClass.current
    val orientationClass = LocalMimikyuOrientationClass.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isReserved = reservedWidth > 0.dp
    val contentSizeClass = when {
        !isReserved -> windowSizeClass
        screenWidthDp - reservedWidth.value >= EXPANDED_MIN_WIDTH_DP -> MimikyuWindowSizeClass.Expanded
        else -> MimikyuWindowSizeClass.Compact
    }
    val contentOrientationClass = if (isReserved) MimikyuOrientationClass.Portrait else orientationClass
    CompositionLocalProvider(
        LocalMimikyuWindowSizeClass provides contentSizeClass,
        LocalMimikyuOrientationClass provides contentOrientationClass,
        content = content
    )
}
