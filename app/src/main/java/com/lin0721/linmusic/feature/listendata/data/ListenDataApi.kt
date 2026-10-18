package com.lin0721.linmusic.feature.listendata.data

import com.lin0721.linmusic.core.model.EmptyBody
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import retrofit2.http.Body
import retrofit2.http.POST

// Listening Stats（） of  Retrofit 。 api， weapi 。
interface ListenDataApi {

    // （）
    @POST("/weapi/content/activity/listen/data/total")
    suspend fun getTotalDuration(
        @Body body: EmptyBody = EmptyBody()
    ): ListenTotalResponse

    // Top Played（）
    @POST("/weapi/content/activity/listen/data/today/song/play/rank")
    suspend fun getTodayRank(
        @Body body: EmptyBody = EmptyBody()
    ): TodayRankResponse

    // /Top Played Top20（）
    @POST("/weapi/content/activity/listen/data/song/play/rank")
    suspend fun getSongPlayRank(
        @Body body: ListenPeriodRequest
    ): SongPlayRankResponse

    // /Listening Report（）。type=year Back， getYearReport
    @POST("/weapi/content/activity/listen/data/report")
    suspend fun getReport(
        @Body body: ListenPeriodRequest
    ): ListenReportResponse

    // Yearly History（）
    @POST("/weapi/content/activity/listen/data/year/report")
    suspend fun getYearReport(
        @Body body: EmptyBody = EmptyBody()
    ): YearReportResponse
}

// =======================  =======================

// endTime Current
@Serializable
data class ListenPeriodRequest(
    val type: String
)

// =======================  =======================

@Serializable
data class ListenTotalResponse(
    val code: Int = 0,
    val data: ListenTotalData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class ListenTotalData(

    val totalDuration: Long = 0
)

// =======================  =======================

@Serializable
data class TodayRankResponse(
    val code: Int = 0,
    val data: TodayRankData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

// Back
@Serializable
data class TodayRankData(
    val songDTOs: List<TodaySongDto> = emptyList()
)

@Serializable
data class TodaySongDto(
    val songId: Long = 0,
    val songName: String = "",
    val picUrl: String = "",
    val artists: List<ListenArtistDto> = emptyList()
)

// ======================= / =======================

@Serializable
data class SongPlayRankResponse(
    val code: Int = 0,
    val data: SongPlayRankData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class SongPlayRankData(
    val songCount: Int = 0,
    val songItems: List<RankSongDto> = emptyList()
)

@Serializable
data class RankSongDto(
    val songId: Long = 0,
    val songName: String = "",
    val albumName: String = "",
    val picUrl: String = "",
    val playCount: Int = 0,
    val artists: List<ListenArtistDto> = emptyList()
)

@Serializable
data class ListenArtistDto(
    val artistId: Long = 0,
    val artistName: String = ""
)

// =======================  =======================

@Serializable
data class YearReportResponse(
    val code: Int = 0,
    val data: YearReportData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class YearReportData(
    val displayYear: Int = 0,
    val yearItems: List<YearItemDto> = emptyList()
)

@Serializable
data class YearItemDto(
    val year: Int = 0,
    val playNum: Int = 0,

    val playDuration: Long = 0
)

// ======================= Listening Report =======================

@Serializable
data class ListenReportResponse(
    val code: Int = 0,
    val data: ListenReportData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

// ， null（ of  topEmotionBlock）。
// of  and ， KeyWord ， JsonNames 。
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ListenReportData(
    val listenTimeBlock: ListenTimeBlockDto? = null,
    val listenTimeDistributionBlock: ListenDistributionBlockDto? = null,
    val wallpaperBlock: WallpaperBlockDto? = null,
    val topSongBlock: TopSongBlockDto? = null,
    val topArtistBlock: TopArtistBlockDto? = null,
    val topStyleBlock: TopStyleBlockDto? = null,
    val topAgeBlock: TopAgeBlockDto? = null,
    val topLanguageBlock: TopLanguageBlockDto? = null,
    val vipBlock: VipBlockDto? = null,
    val djListenDataBlock: DjListenBlockDto? = null,
    @JsonNames("friendsListenWeekBlock", "friendsListenMonthBlock")
    val friendsListenBlock: FriendsListenBlockDto? = null,
    @JsonNames("friendsKeywordWeekBlock", "friendsKeyWordMonthBlock")
    val friendsKeywordBlock: FriendsKeywordBlockDto? = null
)

@Serializable
data class ListenTimeBlockDto(
    // ，
    val playDuration: Int = 0,
    // ， of ，
    val playDurationText: String = "",
    val circleTimePeriodDurations: List<TimePeriodDto> = emptyList()
)

@Serializable
data class TimePeriodDto(
    // early_morning / morning / noon / afternoon / night / deep_night
    val period: String = "",
    val duration: Int = 0
)

@Serializable
data class ListenDistributionBlockDto(
    val listenDays: Int = 0,
    val achievementTitle: AchievementTitleDto? = null,
    val durationDetails: List<DayDurationDto> = emptyList()
)

@Serializable
data class AchievementTitleDto(
    val mainTitle: String = "",
    val subTitle: String = ""
)

@Serializable
data class DayDurationDto(
    // yyyy-MM-dd
    val period: String = "",
    val duration: Int = 0
)

// of Songs， 20
@Serializable
data class WallpaperBlockDto(
    val songCount: Int = 0,
    val picUrls: List<String> = emptyList()
)

// of ： / Save / ，field  and  text
@Serializable
data class TopSongBlockDto(
    val sections: List<TopSongSectionDto> = emptyList()
)

@Serializable
data class TopSongSectionDto(
    val songId: Long = 0,
    val songName: String = "",
    val picUrl: String = "",
    val field: String = "",
    val text: String = ""
)

@Serializable
data class TopArtistBlockDto(
    val sections: List<TopArtistSectionDto> = emptyList()
)

@Serializable
data class TopArtistSectionDto(
    val artistId: Long = 0,
    val artistName: String = "",
    val picUrl: String = "",
    // 「26」
    val text: String = ""
)

@Serializable
data class TopStyleBlockDto(
    val genreName: String = ""
)

@Serializable
data class TopAgeBlockDto(
    val sections: List<TopAgeSectionDto> = emptyList()
)

@Serializable
data class TopAgeSectionDto(
    // 「2000」
    val age: String = "",
    val playSongNum: Int = 0
)

@Serializable
data class TopLanguageBlockDto(
    val sections: List<TopLanguageSectionDto> = emptyList()
)

@Serializable
data class TopLanguageSectionDto(
    val language: String = "",
    val playSongNum: Int = 0
)

@Serializable
data class VipBlockDto(
    val sections: List<VipSectionDto> = emptyList()
)

@Serializable
data class VipSectionDto(
    // vipSongCount /  /
    val type: String = "",
    val field: String = "",
    val mainText: String = "",
    val subText: String = ""
)

@Serializable
data class DjListenBlockDto(

    val podcastPlayDuration: Int = 0,
    val podcastEpNum: Int = 0,
    val audiobookPlayDuration: Int = 0,
    val audiobookEpNum: Int = 0
)

@Serializable
data class FriendsListenBlockDto(
    val items: List<FriendListenItemDto> = emptyList()
)

@Serializable
data class FriendListenItemDto(
    val userId: Long = 0,
    val username: String = "",
    val userAvatar: String = "",
    val songId: Long = 0,
    val songName: String = "",
    val songPicUrl: String = "",
    val playCount: Int = 0
)

@Serializable
data class FriendsKeywordBlockDto(
    val items: List<FriendKeywordItemDto> = emptyList()
)

@Serializable
data class FriendKeywordItemDto(
    val title: String = "",
    val subTitle: String = ""
)
