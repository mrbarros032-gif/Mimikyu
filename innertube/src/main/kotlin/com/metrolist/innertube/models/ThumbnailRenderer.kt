package com.metrolist.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class ThumbnailRenderer(
    val musicThumbnailRenderer: MusicThumbnailRenderer?,
    val musicAnimatedThumbnailRenderer: MusicAnimatedThumbnailRenderer?,
    val croppedSquareThumbnailRenderer: MusicThumbnailRenderer?,
) {
    @Serializable
    data class MusicThumbnailRenderer(
        val thumbnail: Thumbnails,
        val thumbnailCrop: String?,
        val thumbnailScale: String?,
    ) {
        fun getThumbnailUrl() = thumbnail.thumbnails.lastOrNull()?.url
    }

    fun getThumbnailUrl(): String? =
        croppedSquareThumbnailRenderer?.getThumbnailUrl()
            ?: musicThumbnailRenderer?.getThumbnailUrl()
            ?: musicAnimatedThumbnailRenderer?.backupRenderer?.getThumbnailUrl()

    @Serializable
    data class MusicAnimatedThumbnailRenderer(
        val animatedThumbnail: Thumbnails,
        val backupRenderer: MusicThumbnailRenderer,
    )
}
