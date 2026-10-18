package com.lin0721.linmusic.feature.newworks.domain

data class NewWorksMv(
    val id: Long,
    val name: String,
    val coverUrl: String,
    val durationMs: Long,
    val playCount: Long,
    val artistName: String
)

data class NewWorksRelease(
    // Tracks：Songs id，Play；Albums：albumId，AlbumsDetails
    val id: Long,
    val title: String,
    val coverUrl: String,
    val artistName: String,
    val isAlbum: Boolean,
    // Tracks 1，Albums of  albumSongCount
    val trackCount: Int
)

data class NewWorksReleasePage(
    val items: List<NewWorksRelease>,
    val hasMore: Boolean,
    val nextCursor: Long
)
