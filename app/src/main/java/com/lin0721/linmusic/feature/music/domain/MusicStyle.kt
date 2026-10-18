package com.lin0721.linmusic.feature.music.domain

import com.lin0721.linmusic.core.model.Track

// Genres，children 。，UI
data class MusicStyle(
    val id: Long,
    val name: String,
    val enName: String,
    // hex， #。Genres， null  UI
    val colorHex: String?,
    val children: List<MusicStyle> = emptyList()
)

// Genres。 and ，，
data class StylePortrait(
    val content: String,
    val dataTip: String
)

// of ，ratio
data class StylePreference(
    val id: Long,
    val name: String,
    val ratio: Int,
    val colorHex: String?
)

// GenresDetails
data class StyleHead(
    val id: Long,
    val name: String,
    val enName: String,
    // Genres， of ， of
    val desc: String,
    val coverUrl: String?,
    val colorHex: String?,
    // GenresBack 999999+ / 1000+ ，
    val songNum: String,
    val artistNum: String,
    // Genres of ， null
    val favouriteSong: Track?,
    val portrait: StylePortrait?
)

// Genres of Playlists
data class StylePlaylistItem(
    val id: Long,
    val name: String,
    val coverUrl: String,
    val playCount: Long
)

// Genres of Artists
data class StyleArtistItem(
    val id: Long,
    val name: String,
    val picUrl: String,
    val musicSize: Int
)

// Genres of
data class StyleContent(
    val head: StyleHead?,
    val playlists: List<StylePlaylistItem>,
    val songs: List<Track>,
    val artists: List<StyleArtistItem>
)
