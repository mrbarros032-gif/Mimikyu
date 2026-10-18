package com.lin0721.linmusic.core.metadata

import coil.intercept.Interceptor
import coil.request.ImageResult
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry

/**
 * Global Coil Interceptor that intercepts all image load requests across the application.
 *
 * Ensures that:
 * 1. Low-resolution YouTube Music thumbnails (e.g. =w120-h120, =s60-c) are upscaled to 1200x1200.
 * 2. YouTube video thumbnails (ytimg.com) are upscaled to maxresdefault.jpg.
 * 3. Incompatible "?param=" suffixes appended to Google/YouTube URLs are stripped and upscaled.
 * 4. NetEase thumbnails are requested at crisp 500x500 resolution.
 */
class ThumbnailUpscaleInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val data = request.data

        if (data is String && data.isNotBlank()) {
            val resolvedUrl = resolveUrl(data)
            if (resolvedUrl != data) {
                val newRequest = request.newBuilder()
                    .data(resolvedUrl)
                    .build()
                return chain.proceed(newRequest)
            }
        }

        return chain.proceed(request)
    }

    companion object {
        fun resolveUrl(url: String): String {
            if (url.isBlank()) return url
            if (url.startsWith("android.resource://")) {
                return if (url.contains("?")) url.substringBefore("?") else url
            }

            // 1. YouTube / Google UserContent
            if (url.contains("googleusercontent.com") || url.contains("ggpht.com")) {
                val clean = if (url.contains("?param=")) url.substringBefore("?param=") else url
                return YouTubeThumbnailUpscaler.upscale(clean) ?: clean
            }

            // 2. YouTube video thumbnail (i.ytimg.com)
            if (url.contains("i.ytimg.com") || url.contains("img.youtube.com")) {
                val clean = if (url.contains("?param=")) url.substringBefore("?param=") else url
                return YouTubeThumbnailUpscaler.upscale(clean) ?: clean
            }

            // 3. NetEase CDN - upgrade tiny thumbnails to 500x500
            if (url.contains("163.com") && url.contains("?param=")) {
                val base = url.substringBefore("?param=")
                return "$base?param=500y500"
            }

            return url
        }
    }
}
