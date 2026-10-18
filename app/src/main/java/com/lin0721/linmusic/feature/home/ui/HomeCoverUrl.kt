package com.lin0721.linmusic.feature.home.ui

import com.lin0721.linmusic.core.metadata.YouTubeThumbnailUpscaler

// of ：，Playlists ?imageView=1&thumbnail=8。
// YouTube Music / Google CDN ， 1200x1200 。
internal fun String.withCoverParam(param: String): String {
    if (isBlank()) return ""
    if (contains("googleusercontent.com") || contains("ggpht.com") || contains("ytimg.com")) {
        val clean = if (contains("?param=")) substringBefore("?param=") else this
        return YouTubeThumbnailUpscaler.upscale(clean) ?: clean
    }
    if (!contains("163.com")) return this
    return if (contains('?')) this else "$this?param=$param"
}

/**
 * Safely format avatar URLs. Never appends ?param= to Google account or non-NetEase photo URLs.
 */
fun String.formatAvatarUrl(param: String = "200y200"): String {
    if (isBlank()) return ""
    if (contains("googleusercontent.com") || contains("ggpht.com") || !contains("163.com")) {
        return this
    }
    return if (contains('?')) this else "$this?param=$param"
}
