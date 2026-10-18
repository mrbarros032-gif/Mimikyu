package com.lin0721.linmusic.core.player

import android.content.Context
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest

// Coil preloader
class TrackCoverPreloader(private val context: Context) {

    fun preload(coverUrl: String) {
        if (coverUrl.isBlank()) return
        val imageRequest = ImageRequest.Builder(context)
            .data(coverUrl)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
        Coil.imageLoader(context).enqueue(imageRequest)
    }
}
