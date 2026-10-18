package com.lin0721.linmusic.core.ui.theme

import androidx.compose.ui.graphics.Color

// HSV ，/，。
// coerceIn ：/ ≤ 0.35，
// ，；"Darker"，
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

// HSV ； 0（/），，
// of
fun Color.saturate(amount: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt(), hsv)
    if (hsv[1] <= 0f) return this
    hsv[1] = (hsv[1] + (1f - hsv[1]) * amount).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

// base ： base  NEUTRAL_BASE_CHROMA  of ，；
// ， of
fun Color.saturateIfChromatic(amount: Float): Color {
    val chroma = maxOf(red, green, blue) - minOf(red, green, blue)
    val weight = ((chroma - NEUTRAL_BASE_CHROMA) / (MIN_BASE_CHROMA - NEUTRAL_BASE_CHROMA)).coerceIn(0f, 1f)
    return saturate(amount * weight)
}

val BackgroundDark = Color(0xFF141414)
val SurfaceDark = Color(0xFF1A1A1A)
val SurfaceLight = Color(0xFF262626)
val TextGray = Color(0xFFB3B3B3)

// Official Mimikyu "Magenta X Charcoal" Color Palette
val MimikyuMagenta = Color(0xFFFF007F)
val MimikyuDarkMagenta = Color(0xFFD6006B)
val MimikyuCharcoal = Color(0xFF1A1A1A)
val MimikyuDarkCharcoal = Color(0xFF121212)
val MimikyuMediumCharcoal = Color(0xFF282828)
val MimikyuLightGray = Color(0xFFB3B3B3)
val MimikyuWhite = Color(0xFFFFFFFF)

// Core palette aliases mapped to Magenta X Charcoal
val MimikyuGreen = MimikyuMagenta
val MimikyuDarkGreen = MimikyuDarkMagenta
val MimikyuBlack = MimikyuCharcoal
val MimikyuDarkGray = MimikyuCharcoal
val MimikyuMediumGray = MimikyuMediumCharcoal

// Brand Color Palette (Synchronized with Mimikyu)
val OctoberRed = MimikyuMagenta
val OctoberCrimson = MimikyuDarkMagenta
val OctoberCharcoal = MimikyuCharcoal
val OctoberWhite = MimikyuWhite

val NeteaseRed = MimikyuMagenta
val GradientStart = MimikyuCharcoal
val BackgroundBlack = Color(0xFF121212)

// Category Navigation Pill Selected
val NavPillSelected = Color(0xFF331A26)

// Filter Pill Inactive
val FilterPillInactive = Color(0xFF262626)

// Toast
val ToastBackground = MimikyuCharcoal

// Web Login Background
val WebLoginBackground = Color(0xFF121212)

// Fallback base
val FallbackBase = MimikyuCharcoal

// Playlists/Artists placeholder
val CoverPlaceholderDark = MimikyuCharcoal

// Sleep Timer warning
val TimerWarningRed = Color(0xFFFF5252)

// Library gradient
val LibraryVioletGradient = listOf(
    Color(0xFFFF007F),
    Color(0xFFA855F7),
    Color(0xFF6366F1)
)

// Library Blue Green gradient
val LibraryBlueGreenGradient = listOf(
    Color(0xFFFF007F),
    Color(0xFF8B5CF6)
)

// Download
val DownloadedGreen = MimikyuMagenta

// SVIP
val SvipGold = Color(0xFFD4A017)

// of 。Heartbeat Mode、，

val EntryDailyGradient = listOf(Color(0xFFE5484D), Color(0xFF8C1116))
val EntryHotGradient = listOf(Color(0xFFF08C1A), Color(0xFF9C4A05))
val EntryHeartGradient = listOf(Color(0xFFA33FC4), Color(0xFF4B1268))
val EntryRadarGradient = listOf(Color(0xFF2F7FD6), Color(0xFF123F70))
val EntryRoamingGradient = listOf(Color(0xFF3FAE5C), Color(0xFF175C2C))
