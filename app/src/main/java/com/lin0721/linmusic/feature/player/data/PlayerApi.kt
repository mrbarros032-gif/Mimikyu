package com.lin0721.linmusic.feature.player.data

import com.lin0721.linmusic.core.model.Track
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

// PlayDetails（SongsDetails//） of  Retrofit 。
interface PlayerApi {

    @POST("/eapi/v3/song/detail")
    suspend fun getSongDetail(
        @Body body: SongDetailRequest
    ): SongDetailResponse

    // Songs
    @POST("/weapi/song/play/about/block/page")
    suspend fun getSongWikiSummary(
        @Body body: SongWikiSummaryRequest
    ): SongWikiSummaryResponse

    // Songs
    @POST("/weapi/song/creators")
    suspend fun getSongCreators(
        @Body body: SongCreatorsRequest
    ): SongCreatorsResponse

    // （）
    @POST("/eapi/song/chorus")
    suspend fun getSongChorus(
        @Body body: SongChorusRequest
    ): SongChorusResponse
}

// ======================= SongsDetails DTO =======================

@Serializable
data class SongDetailRequest(
    val c: String
)

@Serializable
data class SongDetailResponse(
    val code: Int = 0,
    val songs: List<Track> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

// ======================= Songs DTO =======================

@Serializable
data class SongWikiSummaryRequest(
    val songId: Long
)

@Serializable
data class SongWikiSummaryResponse(
    val code: Int = 0,
    val data: SongWikiSummaryData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class SongWikiSummaryData(
    val blocks: List<SongWikiBlock> = emptyList()
)

@Serializable
data class SongWikiBlock(
    val code: String = "",
    val showType: String = "",
    val creatives: List<SongWikiCreative> = emptyList()
)

@Serializable
data class SongWikiCreative(
    val creativeType: String = "",
    val resources: List<SongWikiResource> = emptyList(),
    val uiElement: SongWikiUiElement? = null
)

@Serializable
data class SongWikiResource(
    // FIRST_LISTEN / TOTAL_PLAY
    val resourceType: String = "",
    val resourceExt: SongWikiResourceExt? = null,
    val uiElement: SongWikiUiElement? = null
)

@Serializable
data class SongWikiUiElement(
    val mainTitle: SongWikiMainTitle? = null,
    val textLinks: List<SongWikiTextLink> = emptyList(),
    // of ，「3」
    val buttons: List<SongWikiButton> = emptyList()
)

@Serializable
data class SongWikiButton(
    val text: String = ""
)

@Serializable
data class SongWikiResourceExt(
    val musicFirstListenDto: MusicFirstListenDto? = null,
    val musicTotalPlayDto: MusicTotalPlayDto? = null
)

// date 「2026.08.06 22:39」，season/period 「」「Late Night」
@Serializable
data class MusicFirstListenDto(
    val date: String = "",
    val season: String = "",
    val period: String = ""
)

// text  of ，「3600 of 」
@Serializable
data class MusicTotalPlayDto(
    val playCount: Int = 0,
    val text: String = ""
)

@Serializable
data class SongWikiMainTitle(
    val title: String = ""
)

@Serializable
data class SongWikiTextLink(
    val text: String = ""
)

// ======================= Songs DTO =======================

@Serializable
data class SongCreatorsRequest(
    val songId: Long
)

@Serializable
data class SongCreatorsResponse(
    val code: Int = 0,
    val data: SongCreatorsData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class SongCreatorsData(
    val songCreatorsRoleVos: List<SongCreatorRole> = emptyList()
)

@Serializable
data class SongCreatorRole(
    val roleName: String = "",
    val creatorMetaVOS: List<CreatorMeta> = emptyList()
)

@Serializable
data class CreatorMeta(
    val artistName: String = ""
)

// =======================  DTO =======================

// ids  JSON  of ， "[186016]"
@Serializable
data class SongChorusRequest(
    val ids: String
)

@Serializable
data class SongChorusResponse(
    val code: Int = 0,
    val chorus: List<SongChorusItem> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

// startTime/endTime
@Serializable
data class SongChorusItem(
    val id: Long = 0L,
    val startTime: Long = 0L,
    val endTime: Long = 0L
)
