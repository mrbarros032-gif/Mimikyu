package com.lin0721.linmusic.core.ui.theme.vibrant

// 5-bit ， vibrant-image/src/histogram.ts  of  sigBits=5 。
// pixels  ARGB_8888 packed int （bitmap.getPixels  of ）
internal class VibrantHistogram(
    pixels: IntArray,
    filter: (r: Int, g: Int, b: Int, a: Int) -> Boolean,
) {
    companion object {
        const val SIG_BITS = 5
        const val RSHIFT = 8 - SIG_BITS
    }

    val hist = IntArray(1 shl (3 * SIG_BITS))
    var rMin = Int.MAX_VALUE
        private set
    var rMax = 0
        private set
    var gMin = Int.MAX_VALUE
        private set
    var gMax = 0
        private set
    var bMin = Int.MAX_VALUE
        private set
    var bMax = 0
        private set

    init {
        for (pixel in pixels) {
            val a = (pixel ushr 24) and 0xFF
            val r = (pixel ushr 16) and 0xFF
            val g = (pixel ushr 8) and 0xFF
            val b = pixel and 0xFF
            if (!filter(r, g, b, a)) continue

            val rq = r ushr RSHIFT
            val gq = g ushr RSHIFT
            val bq = b ushr RSHIFT
            hist[colorIndex(rq, gq, bq)]++

            if (rq > rMax) rMax = rq
            if (rq < rMin) rMin = rq
            if (gq > gMax) gMax = gq
            if (gq < gMin) gMin = gq
            if (bq > bMax) bMax = bq
            if (bq < bMin) bMin = bq
        }
    }

    // filter  rMin ， > rMax，"Empty histogram"
    fun isEmpty(): Boolean = rMin > rMax

    fun colorIndex(r: Int, g: Int, b: Int): Int = (r shl (2 * SIG_BITS)) + (g shl SIG_BITS) + b
}
