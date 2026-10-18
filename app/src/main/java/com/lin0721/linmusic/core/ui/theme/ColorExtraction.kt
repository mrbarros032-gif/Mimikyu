package com.lin0721.linmusic.core.ui.theme

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.ui.theme.vibrant.VibrantSwatch
import com.lin0721.linmusic.core.ui.theme.vibrant.defaultVibrantFilter
import com.lin0721.linmusic.core.ui.theme.vibrant.hslToRgb
import com.lin0721.linmusic.core.ui.theme.vibrant.medianCutQuantize
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ColorExtraction"

// Play of ：base/textHighlight Play UI  of
data class PlayerBackdropPalette(
    val base: Color,
    val textHighlight: Color,
)

// candidate swatch ，/，。
// （RGB ） HSL ：HSL  0 or 1
// ， 1  of "High Saturation"，
private const val MEANINGFUL_CHROMA = 0.06f

// of ，Play and Play
val FallbackBackdropPalette = PlayerBackdropPalette(
    base = FallbackBase,
    textHighlight = lerp(FallbackBase, Color.White, 0.85f),
)

// node-vibrant ：64 、quality=5
private const val VIBRANT_COLOR_COUNT = 64
private const val VIBRANT_QUALITY = 5

// of （）， ArtistBackdrop  of  640 ，
private const val DECODE_MAX_DIMENSION = 640

// Palette extraction strictly runs on Dispatchers.Default, NEVER blocking the main thread
suspend fun extractBackdropPaletteFromUrl(context: Context, url: String): PlayerBackdropPalette = withContext(Dispatchers.Default) {
    val canonicalUrl = url.substringBefore("?param=")
    if (canonicalUrl.isBlank()) return@withContext FallbackBackdropPalette
    try {
        val request = ImageRequest.Builder(context)
            .data(canonicalUrl)
            .size(DECODE_MAX_DIMENSION, DECODE_MAX_DIMENSION)
            .allowHardware(false)
            .build()
        val drawable = context.imageLoader.execute(request).drawable ?: return@withContext FallbackBackdropPalette
        extractBackdropPalette(drawable)
    } catch (e: Exception) {
        AppLogger.d(TAG, "Color extraction request failed, using default dark palette", e)
        FallbackBackdropPalette
    }
}

suspend fun extractBaseColorFromUrl(context: Context, url: String): Color {
    return extractBackdropPaletteFromUrl(context, url).base
}

suspend fun extractBackdropPaletteFromBitmap(bitmap: Bitmap): PlayerBackdropPalette = withContext(Dispatchers.Default) {
    extractBackdropPaletteFromBitmapSync(bitmap)
}

fun extractBackdropPalette(drawable: android.graphics.drawable.Drawable): PlayerBackdropPalette {
    return try {
        val bitmap = drawable.toBitmap()
        extractBackdropPaletteFromBitmapSync(bitmap)
    } catch (e: Exception) {
        AppLogger.d(TAG, "Color extraction failed, using default dark palette", e)
        FallbackBackdropPalette
    }
}

fun extractBackdropPaletteFromBitmapSync(bitmap: Bitmap): PlayerBackdropPalette {
    return try {
        val scaled = scaleDownByQuality(bitmap, VIBRANT_QUALITY)
        val pixels = IntArray(scaled.width * scaled.height)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)

        val quantized = medianCutQuantize(pixels, VIBRANT_COLOR_COUNT, ::defaultVibrantFilter)
        if (quantized.isEmpty()) return FallbackBackdropPalette

        // No chroma signal
        if (isGrayscaleSwatches(quantized)) {
            val base = grayscaleBaseColor(quantized)
            return PlayerBackdropPalette(
                base = base,
                textHighlight = lerp(base, Color.White, 0.85f),
            )
        }

        val base = pickBaseColor(quantized)
        PlayerBackdropPalette(
            base = base,
            textHighlight = lerp(base, Color.White, 0.85f),
        )
    } catch (e: Exception) {
        AppLogger.d(TAG, "Color extraction failed, using default dark palette", e)
        FallbackBackdropPalette
    }
}

// node-vibrant  of  Image.scaleDown： 1/quality ，
private fun scaleDownByQuality(bitmap: Bitmap, quality: Int): Bitmap {
    val ratio = 1f / quality
    val targetWidth = (bitmap.width * ratio).toInt().coerceAtLeast(1)
    val targetHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}

// quantized swatch ； swatch "No chroma signal"
internal fun isGrayscaleSwatches(swatches: List<VibrantSwatch>): Boolean =
    swatches.none { it.chroma >= MEANINGFUL_CHROMA }

// of ：（） of ，
// Play、 of
internal fun grayscaleBaseColor(swatches: List<VibrantSwatch>): Color {
    val dominant = swatches.maxByOrNull { it.population } ?: return FallbackBase
    val lightness = dominant.hsl[2].coerceIn(0.15f, 0.35f)
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(0f, 0f, lightness)))
}

// and ：/ and ， and
private const val PAPER_WHITE_LIGHTNESS = 0.9f
private const val INK_BLACK_LIGHTNESS = 0.1f

// of /（ AIR  of ），。
// of （ 0.09）， NEUTRAL_FULL_LIGHTNESS
private const val NEUTRAL_CHROMA = 0.13f
private const val NEUTRAL_FULL_LIGHTNESS = 0.5f

// ， of 。
// 1.45  and  1.71（AIR）
private const val NEUTRAL_WIN_RATIO = 1.6f

// ，hsl[0]  0..1
private const val HUE_GROUP_RADIUS = 20f / 360f

// of ，
private const val DARK_PREFERENCE_POWER = 2

// base ，/（ Mimikyu ），
// 、
internal const val MIN_BASE_CHROMA = 0.15f
private const val MAX_BASE_CHROMA = 0.30f
private const val MIN_BASE_LIGHTNESS = 0.2f
private const val MAX_BASE_LIGHTNESS = 0.5f


internal const val NEUTRAL_BASE_CHROMA = 0.06f

// Mimikyu ： of （ NEUTRAL_WIN_RATIO  and ），
// of
internal fun pickBaseColor(swatches: List<VibrantSwatch>): Color {
    val candidates = swatches.filter {
        it.population > 0 && it.hsl[2] >= INK_BLACK_LIGHTNESS && it.hsl[2] < PAPER_WHITE_LIGHTNESS
    }
    if (candidates.isEmpty()) return FallbackBase

    val (neutral, chromatic) = candidates.partition {
        it.chroma < NEUTRAL_CHROMA * min(1f, it.hsl[2] / NEUTRAL_FULL_LIGHTNESS)
    }
    val neutralArea = neutral.sumOf { it.population }

    var bestGroup = emptyList<VibrantSwatch>()
    var bestArea = 0
    for (seed in chromatic) {
        val group = chromatic.filter { hueDistance(seed.hsl[0], it.hsl[0]) <= HUE_GROUP_RADIUS }
        val area = group.sumOf { it.population }
        if (area > bestArea) {
            bestGroup = group
            bestArea = area
        }
    }

    if (bestGroup.isEmpty() || neutralArea >= bestArea * NEUTRAL_WIN_RATIO) {
        val lightness = neutral.sumOf { it.hsl[2].toDouble() * it.population }.toFloat() / neutralArea
        val hue = weightedMeanHue(neutral)
            ?: return lightness.coerceIn(MIN_BASE_LIGHTNESS, MAX_BASE_LIGHTNESS).let { Color(it, it, it) }
        return normalizedColor(hue, NEUTRAL_BASE_CHROMA, lightness)
    }

    val chosen = bestGroup.maxBy { swatch ->
        var darkness = 1f
        repeat(DARK_PREFERENCE_POWER) { darkness *= 1f - swatch.hsl[2] }
        swatch.population * darkness
    }
    return normalizedColor(
        chosen.hsl[0],
        chosen.chroma.coerceIn(MIN_BASE_CHROMA, MAX_BASE_CHROMA),
        chosen.hsl[2],
    )
}

// HSL ：， of
private fun normalizedColor(hue: Float, chroma: Float, lightness: Float): Color {
    val l = lightness.coerceIn(MIN_BASE_LIGHTNESS, MAX_BASE_LIGHTNESS)
    val s = (chroma / (1f - abs(2f * l - 1f))).coerceIn(0f, 1f)
    val (r, g, b) = hslToRgb(hue, s, l)
    return Color(r, g, b)
}

// ，×；， 0
private fun weightedMeanHue(swatches: List<VibrantSwatch>): Float? {
    var x = 0.0
    var y = 0.0
    for (swatch in swatches) {
        val weight = swatch.population.toDouble() * swatch.chroma
        val angle = swatch.hsl[0] * 2.0 * PI
        x += weight * cos(angle)
        y += weight * sin(angle)
    }
    if (x == 0.0 && y == 0.0) return null
    val hue = (atan2(y, x) / (2.0 * PI)).toFloat()
    return if (hue < 0f) hue + 1f else hue
}

private fun hueDistance(a: Float, b: Float): Float {
    val d = abs(a - b) % 1f
    return min(d, 1f - d)
}

object PaletteMemoryCache {
    private val cache = android.util.LruCache<String, PlayerBackdropPalette>(150)

    fun get(mediaId: String): PlayerBackdropPalette? = cache.get(mediaId)

    fun put(mediaId: String, palette: PlayerBackdropPalette) {
        cache.put(mediaId, palette)
    }
}
