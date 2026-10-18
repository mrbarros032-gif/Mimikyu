package com.lin0721.linmusic.core.metadata

/**
 * Utility for upscaling YouTube Music and video thumbnails to high resolution.
 *
 * For YouTube Music (Google UserContent / ggpht), dynamically replaces the size suffix
 * with `=w1200-h1200-l90-rj` (or `=w544-h544-l90-rj`).
 * For YouTube video thumbnails (ytimg.com), replaces standard/low-res filenames
 * with maxresdefault.jpg.
 */
object YouTubeThumbnailUpscaler {

    private val YTIMG_REGEX = Regex("""/(default|mqdefault|hqdefault|sddefault)\.jpg""")

    /**
     * Upscales a thumbnail URL to high resolution (1200x1200 for usercontent or maxresdefault for ytimg).
     */
    fun upscale(url: String?): String? {
        if (url.isNullOrBlank()) return null

        // 1. Google UserContent / ggpht (YouTube Music art)
        if (url.contains("googleusercontent.com") || url.contains("ggpht.com")) {
            return if (url.contains("=")) {
                val base = url.substringBefore("=")
                "$base=w1200-h1200-l90-rj"
            } else {
                "$url=w1200-h1200-l90-rj"
            }
        }

        // 2. YouTube video thumbnails (i.ytimg.com / img.youtube.com)
        if (url.contains("i.ytimg.com") || url.contains("img.youtube.com")) {
            return url.replace(YTIMG_REGEX, "/maxresdefault.jpg")
        }

        // 3. Strip any query params if Netease CDN
        if (url.contains("163.com") && url.contains("?param=")) {
            return url.substringBefore("?param=")
        }

        return url
    }
}
