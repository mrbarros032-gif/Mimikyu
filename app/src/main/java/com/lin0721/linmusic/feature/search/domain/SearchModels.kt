package com.lin0721.linmusic.feature.search.domain


data class HotSearch(
    val keyword: String,
    val score: Int,
    val description: String = "",
    val iconUrl: String? = null
)

// Playlists
data class PlaylistTag(
    val name: String,
    val coverUrl: String = ""
)
