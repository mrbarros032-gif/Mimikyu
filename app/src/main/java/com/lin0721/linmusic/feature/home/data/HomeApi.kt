package com.lin0721.linmusic.feature.home.data

import com.lin0721.linmusic.core.model.Album
import com.lin0721.linmusic.core.model.Artist
import com.lin0721.linmusic.core.model.EmptyBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

// / of  Retrofit 。
interface HomeApi {

    // ： of ， cursor 。
    // Back，。 eapi —— weapi  cursor，。
    @POST("/eapi/homepage/block/page")
    suspend fun getHomeBlockPage(
        @Body body: HomeBlockPageRequest = HomeBlockPageRequest()
    ): HomeBlockPageResponse

    // RecommendedPlaylists（，）
    @POST("/eapi/personalized/playlist")
    suspend fun getPersonalizedPlaylists(
        @Body body: PersonalizedRequest = PersonalizedRequest()
    ): PersonalizedResponse

    // Daily RecommendationsSongs（， 06:00 ）
    @POST("/eapi/v3/discovery/recommend/songs")
    suspend fun getDailyRecommendSongs(
        @Body body: EmptyBody = EmptyBody()
    ): DailyRecommendSongsResponse

    // of Daily History（ VIP ）
    @POST("/weapi/discovery/recommend/songs/history/recent")
    suspend fun getHistoryRecommendDates(
        @Body body: EmptyBody = EmptyBody()
    ): HistoryDatesResponse

    // of Daily HistoryDetails
    @POST("/weapi/discovery/recommend/songs/history/detail")
    suspend fun getHistoryRecommendDetail(
        @Body body: HistoryDetailRequest
    ): HistoryDetailResponse

    // Details（Songs）
    @POST("/eapi/toplist/detail")
    suspend fun getToplistDetail(
        @Body body: EmptyBody = EmptyBody()
    ): ToplistDetailResponse
}

// ==================== Recommended ====================

@Serializable
data class PersonalizedRequest(
    val limit: Int = 30
)

// Recommended
@Serializable
data class PersonalizedResponse(
    val code: Int = 0,
    val result: List<PersonalizedPlaylist> = emptyList(),
) {
    val isSuccess: Boolean get() = code == 200
}

// RecommendedPlaylists
@Serializable
data class PersonalizedData(
    // RecommendedPlaylists
    val playlists: List<PersonalizedPlaylist> = emptyList(),
)

@Serializable
data class PersonalizedPlaylist(
    val id: Long = 0,
    val name: String = "",
    // Playlist Cover
    val picUrl: String = "",
)

// ======================= Daily RecommendationsSongs =======================

@Serializable
data class DailyRecommendSongsResponse(
    val code: Int = 0,
    val data: DailyRecommendData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class DailyRecommendData(
    val dailySongs: List<DailySong> = emptyList()
)

@Serializable
data class DailySong(
    val id: Long = 0,
    val name: String = "",
    val ar: List<Artist> = emptyList(),
    val al: Album = Album(),
    val fee: Int = 0,
    // Recommended（："Recommended based on what you like"）
    val reason: String? = null
)

// ======================= Daily History =======================

@Serializable
data class HistoryDatesResponse(
    val code: Int = 0,
    val data: HistoryDatesData? = null
)

@Serializable
data class HistoryDatesData(
    @SerialName("dates")
    val list: List<String> = emptyList()
)

@Serializable
data class HistoryDetailRequest(
    val date: String
)

@Serializable
data class HistoryDetailResponse(
    val code: Int = 0,
    val data: HistoryDetailData? = null
)

@Serializable
data class HistoryDetailData(
    @SerialName("songs")
    val dailySongs: List<DailySong> = emptyList()
)

// =======================  DTO =======================

@Serializable
data class ToplistDetailResponse(
    val code: Int = 0,
    val list: List<ToplistDto> = emptyList()
)

@Serializable
data class ToplistDto(
    val id: Long = 0,
    val name: String = "",
    val coverImgUrl: String = "",
    val updateFrequency: String = "",
    // Songs
    val tracks: List<ToplistTrackDto>? = null
)

@Serializable
data class ToplistTrackDto(
    val first: String = "",
    val second: String = "" // Artists
)
