package com.lin0721.linmusic.core.ui.theme

import androidx.compose.ui.graphics.Color

const val NEUTRAL_BASE_CHROMA = 0.05f
const val MIN_BASE_CHROMA = 0.15f

fun Color.lighten(amount: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt(), hsv)
    hsv[2] = (hsv[2] + (1f - hsv[2]) * amount).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun Color.darken(amount: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt(), hsv)
    hsv[2] = (hsv[2] * (1f - amount)).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun Color.saturate(amount: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt(), hsv)
    if (hsv[1] <= 0f) return this
    hsv[1] = (hsv[1] + (1f - hsv[1]) * amount).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun Color.saturateIfChromatic(amount: Float): Color {
    val chroma = maxOf(red, green, blue) - minOf(red, green, blue)
    val weight = ((chroma - NEUTRAL_BASE_CHROMA) / (MIN_BASE_CHROMA - NEUTRAL_BASE_CHROMA)).coerceIn(0f, 1f)
    return saturate(amount * weight)
}

// NYX Color Palette (estilo Milk Player)
val BackgroundDark = Color(0xFF0B0B10)
val SurfaceDark = Color(0xFF16161E)
val SurfaceLight = Color(0xFF21212C)
val TextGray = Color(0xFF9A9AAE)

val NyxAccent = Color(0xFF8B7CFF)
val NyxAccentDark = Color(0xFF6B5CD6)

val MimikyuMagenta = NyxAccent
val MimikyuDarkMagenta = NyxAccentDark
val MimikyuCharcoal = Color(0xFF16161E)
val MimikyuDarkCharcoal = Color(0xFF0B0B10)
val MimikyuMediumCharcoal = Color(0xFF21212C)
val MimikyuLightGray = Color(0xFF9A9AAE)
val MimikyuWhite = Color(0xFFFFFFFF)

val MimikyuGreen = NyxAccent
val MimikyuDarkGreen = NyxAccentDark
val MimikyuBlack = MimikyuCharcoal
val MimikyuDarkGray = MimikyuCharcoal
val MimikyuMediumGray = MimikyuMediumCharcoal

val OctoberRed = NyxAccent
val OctoberCrimson = NyxAccentDark
val OctoberCharcoal = MimikyuCharcoal
val OctoberWhite = MimikyuWhite

val NeteaseRed = NyxAccent
val GradientStart = MimikyuCharcoal
val BackgroundBlack = Color(0xFF0B0B10)

val NavPillSelected = Color(0xFF2A2540)
val FilterPillInactive = Color(0xFF21212C)
val ToastBackground = MimikyuCharcoal
val WebLoginBackground = Color(0xFF0B0B10)
val FallbackBase = MimikyuCharcoal
val CoverPlaceholderDark = MimikyuCharcoal
val TimerWarningRed = Color(0xFFFF5252)

val LibraryVioletGradient = listOf(
    Color(0xFF8B7CFF),
    Color(0xFFA78BFA),
    Color(0xFF6366F1)
)

val LibraryBlueGreenGradient = listOf(
    Color(0xFF8B7CFF),
    Color(0xFF7C3AED)
)

val DownloadedGreen = NyxAccent
val SvipGold = Color(0xFFD4A017)

val EntryDailyGradient = listOf(Color(0xFF8B7CFF), Color(0xFF5B4DB8))
val EntryHotGradient = listOf(Color(0xFFF08C1A), Color(0xFF9C4A05))
val EntryHeartGradient = listOf(Color(0xFFA33FC4), Color(0xFF4B1268))
val EntryRadarGradient = listOf(Color(0xFF2F7FD6), Color(0xFF123F70))
val EntryRoamingGradient = listOf(Color(0xFF3FAE5C), Color(0xFF175C2C))
