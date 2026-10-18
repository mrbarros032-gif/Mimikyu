package com.lin0721.linmusic.feature.home.data

import kotlinx.serialization.Serializable

// /eapi/homepage/block/page  of 。
// of ， and 、 of ：
// resourceId  null；shelf  mainTitle  and  subTitle ；
// labelTexts  and  labelText ；extInfo  of  blockCode 。

@Serializable
data class HomeBlockPageRequest(
    // true，
    val refresh: Boolean = false,
    // ， of  cursor
    val cursor: String = ""
)

@Serializable
data class HomeBlockPageResponse(
    val code: Int = 0,
    val data: HomeBlockPageData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class HomeBlockPageData(
    // ，； null
    val cursor: String? = null,
    val hasMore: Boolean = false,
    val blocks: List<HomeBlockDto> = emptyList()
)

@Serializable
data class HomeBlockDto(
    val blockCode: String = "",
    val showType: String = "",
    val uiElement: HomeUiElementDto? = null,
    val creatives: List<HomeCreativeDto> = emptyList(),
    val sort: Int = 0
)

// creative ， resources  creative
@Serializable
data class HomeCreativeDto(
    val creativeType: String = "",
    val resources: List<HomeResourceDto> = emptyList()
)

@Serializable
data class HomeResourceDto(
    val resourceType: String = "",
    val resourceId: String? = null,
    val uiElement: HomeUiElementDto? = null,
    val action: String? = null,
    val actionType: String? = null,
    // ， of  mainSong ，Details
    val resourceExtInfo: HomeResourceExtInfoDto? = null
)

@Serializable
data class HomeResourceExtInfoDto(
    val djProgram: HomeDjProgramDto? = null
)

@Serializable
data class HomeDjProgramDto(
    val id: Long = 0,
    val name: String = "",
    val coverUrl: String? = null,
    val duration: Long = 0,
    val mainSong: HomeDjMainSongDto? = null,
    val radio: HomeDjRadioDto? = null,
    val dj: HomeDjUserDto? = null
)

@Serializable
data class HomeDjMainSongDto(
    val id: Long = 0
)

@Serializable
data class HomeDjRadioDto(
    val id: Long = 0,
    val name: String = ""
)

@Serializable
data class HomeDjUserDto(
    val nickname: String? = null
)

// block  and  resource  uiElement，
@Serializable
data class HomeUiElementDto(
    val mainTitle: HomeTitleDto? = null,
    val subTitle: HomeTitleDto? = null,
    val description: String? = null,
    val image: HomeImageDto? = null,
    val labelTexts: List<String> = emptyList(),
    val labelText: HomeLabelDto? = null
)

@Serializable
data class HomeTitleDto(
    val title: String? = null
)

@Serializable
data class HomeImageDto(
    val imageUrl: String? = null
)

@Serializable
data class HomeLabelDto(
    val text: String? = null
)

