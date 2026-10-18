package com.lin0721.linmusic.core.ui.theme.vibrant

import androidx.compose.ui.graphics.Color

// node-vibrant  of  Swatch： of  +  of
data class VibrantSwatch(val rgb: Color, val population: Int) {
    val hsl: FloatArray by lazy {
        rgbToHsl((rgb.red * 255f).toInt(), (rgb.green * 255f).toInt(), (rgb.blue * 255f).toInt())
    }

    // ：RGB （0..1），。
    // HSL  0 or 1  (1-|2L-1|) ——
    // 1  of "High Saturation"，，
    val chroma: Float by lazy {
        maxOf(rgb.red, rgb.green, rgb.blue) - minOf(rgb.red, rgb.green, rgb.blue)
    }
}
