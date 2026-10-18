package com.lin0721.linmusic.core.ui.theme.vibrant

// node-vibrant  of  "default" filter（ packages/node-vibrant/src/pipeline/index.ts）：
// ，
internal fun defaultVibrantFilter(r: Int, g: Int, b: Int, a: Int): Boolean =
    a >= 125 && !(r > 250 && g > 250 && b > 250)
