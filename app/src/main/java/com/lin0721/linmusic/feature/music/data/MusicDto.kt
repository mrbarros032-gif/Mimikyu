package com.lin0721.linmusic.feature.music.data

import com.lin0721.linmusic.core.model.Track
import kotlinx.serialization.Serializable

// 「」tab  of Genres， of 。
// of ：songNum/artistNum  "999999+" ；childrenTags  null；
// colorDeep/colorShallow  #  of  hex Genres。

// ==================== Genres ====================

@Serializable
data class StyleListResponse(
    val code: Int = 0,
    val data: List<StyleTagDto> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class StyleTagDto(
    val tagId: Long = 0,
    val tagName: String = "",
    val enName: String? = null,
    val level: Int = 0,
    // Genres 1~29 ，Genres null
    val childrenTags: List<StyleTagDto> = emptyList(),
    val colorDeep: String? = null,
    val colorShallow: String? = null
)

// ====================  of Genres ====================

@Serializable
data class StylePreferenceResponse(
    val code: Int = 0,
    val data: StylePreferenceData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class StylePreferenceData(
    // of ，
    val tagPreferenceVos: List<StylePreferenceItemDto> = emptyList(),
    // and Genres， of
    val tags: List<StyleTagDto> = emptyList()
)

@Serializable
data class StylePreferenceItemDto(
    val tagId: Long = 0,
    val tagName: String = "",
    // ，
    val ratio: String = "",
    val picUrl: String? = null
)

// ==================== GenresDetails ====================

@Serializable
data class StyleHeadResponse(
    val code: Int = 0,
    val data: StyleHeadDto? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class StyleHeadDto(
    val tagId: Long = 0,
    val name: String = "",
    val enName: String? = null,
    val desc: String? = null,
    // URL ， and Genres of
    val cover: List<String> = emptyList(),
    val colorDeep: String? = null,
    val colorShallow: String? = null,
    val songNum: String? = null,
    val artistNum: String? = null,
    val tagPortrait: StyleTagPortraitDto? = null,
    val favouriteSong: StyleFavouriteSongDto? = null
)

// of Genres： ${xxx} ， pattern  key
@Serializable
data class StyleTagPortraitDto(
    val templateContent: String? = null,
    val pattern: Map<String, StylePortraitValueDto> = emptyMap(),
    val dataTip: String? = null
)

@Serializable
data class StylePortraitValueDto(
    val text: String? = null
)

@Serializable
data class StyleFavouriteSongDto(
    val favouriteSong: Track? = null,
    val markHeart: Int = 0
)

// ==================== Genres ====================

@Serializable
data class StylePlaylistResponse(
    val code: Int = 0,
    val data: StylePlaylistData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class StylePlaylistData(
    val playlist: List<StylePlaylistDto> = emptyList(),
    val page: StylePageDto? = null
)

@Serializable
data class StylePlaylistDto(
    val id: Long = 0,
    val name: String = "",
    // imageView ， param
    val cover: String? = null,
    val songCount: Int = 0,
    val userName: String? = null,
    val playCount: Long = 0
)

@Serializable
data class StyleSongResponse(
    val code: Int = 0,
    val data: StyleSongData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class StyleSongData(
    val songs: List<Track> = emptyList(),
    val page: StylePageDto? = null
)

@Serializable
data class StyleArtistResponse(
    val code: Int = 0,
    val data: StyleArtistData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class StyleArtistData(
    val artists: List<StyleArtistDto> = emptyList(),
    val page: StylePageDto? = null
)

@Serializable
data class StyleArtistDto(
    val id: Long = 0,
    val name: String = "",
    val picUrl: String? = null,
    val img1v1Url: String? = null,
    val albumSize: Int = 0,
    val musicSize: Int = 0
)

// of
@Serializable
data class StylePageDto(
    val cursor: Int = 0,
    val size: Int = 0,
    val more: Boolean = false,
    val total: Int = 0
)

// ====================  ====================

@Serializable
data class StyleHeadRequest(
    val tagId: Long
)

@Serializable
data class StyleContentRequest(
    val tagId: Long,
    val cursor: Int = 0,
    val size: Int = 20,
    val sort: Int = 0
)
