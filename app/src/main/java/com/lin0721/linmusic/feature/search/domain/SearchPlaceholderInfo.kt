package com.lin0721.linmusic.feature.search.domain

/**
 * Encapsulates dynamic placeholder metadata for the Search bar.
 * If user searched for a song or album, [imageUrl] contains the album cover and [isArtist] is false.
 * If user searched for an artist, [imageUrl] contains the artist image and [isArtist] is true.
 */
data class SearchPlaceholderInfo(
    val title: String,
    val subtitle: String = "",
    val imageUrl: String = "",
    val isArtist: Boolean = false
)
