package com.lin0721.linmusic.core.player.data

import com.lin0721.linmusic.core.model.Track
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

// Play（Play// and Recommended） of  Retrofit 。
interface PlaybackApi {

    // SongsPlay
    @POST("/eapi/song/enhance/player/url/v1")
    suspend fun getSongUrl(
        @Body body: SongUrlRequest
    ): SongUrlResponse

    @POST("/eapi/v1/discovery/simiSong")
    suspend fun getSimiSongs(
        @Body body: SimiSongRequest
    ): SimiSongResponse

    @POST("/eapi/song/lyric/v1")
    suspend fun getLyrics(
        @Body body: LyricRequest
    ): LyricResponse

    // ================== RecommendedSongs ==================
    @POST("/eapi/playmode/intelligence/list")
    suspend fun getIntelligenceSongs(
        @Body body: IntelligenceSongsRequest
    ): IntelligenceSongsResponse

    // ================== Play（） ==================
    // /eapi/feedback/weblog， startplay/play
    @POST("/eapi/feedback/weblog")
    suspend fun reportWeblog(
        @Body body: WeblogRequest
    ): WeblogResponse
}

// ======================= Play DTO =======================

@Serializable
data class SongUrlRequest(
    val ids: String,
    val level: String = "standard",
    val encodeType: String = "flac"
)

@Serializable
data class SongUrlResponse(
    val code: Int = 0,
    val data: List<SongUrlItem> = emptyList(),
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class SongUrlItem(
    val id: Long = 0,
    val url: String? = null,
    val br: Long = 0,
    val size: Long = 0,
    val md5: String? = null,
    val type: String? = null,
    // VIPSongs，freeTrialInfo
    val freeTrialInfo: FreeTrialInfo? = null,
)

@Serializable
data class FreeTrialInfo(
    val start: Long = 0,
    val end: Long = 0,
)

// =======================  DTO =======================

@Serializable
data class LyricRequest(
    val id: Long,
    val cp: Boolean = false,
    val tv: Int = -1,
    val lv: Int = -1,
    val rv: Int = -1,
    val kv: Int = -1,
    val yv: Int = 99, // 99  YRC
    val ytv: Int = -1,
    val yrv: Int = -1
)

@Serializable
data class LyricResponse(
    val code: Int = 0,
    val lrc: LyricContent? = null,
    val tlyric: LyricContent? = null,
    val ytlrc: LyricContent? = null,    // ：YRC  of
    val romalrc: LyricContent? = null,
    val yrc: LyricContent? = null, // ： of
    val nolyric: Boolean = false, // ， true
    val uncollected: Boolean = false // Songs
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class LyricContent(
    val version: Int = 0,
    val lyric: String? = null
)

// ======================= Songs DTO =======================

@Serializable
data class SimiSongRequest(
    val songid: String
)

@Serializable
data class SimiSongResponse(
    val code: Int = 0,
    val songs: List<Track> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

// ======================= RecommendedSongs DTO =======================

@Serializable
data class IntelligenceSongsRequest(
    val songId: String,
    val playlistId: String,
    val type: String = "fromPlayOne",
    val startMusicId: String,
    val count: Int = 20
)

@Serializable
data class IntelligenceSongsResponse(
    val code: Int = 0,
    val data: List<IntelligenceItem> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class IntelligenceItem(
    val id: Long = 0,
    val recommended: Boolean = false,
    val songInfo: Track? = null
)

// ======================= Play DTO =======================

// logs  of  JSON （），， Repository
@Serializable
data class WeblogRequest(
    val logs: String
)

@Serializable
data class WeblogResponse(
    val code: Int = 0,
    val data: String? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class StartPlayLogEntry(
    val action: String = "startplay",
    val json: StartPlayLogJson
)

@Serializable
data class StartPlayLogJson(
    val id: Long,
    val type: String = "song",
    val mainsite: String = "1",
    val mainsiteWeb: String = "1",
    val content: String
)

@Serializable
data class PlayLogEntry(
    val action: String = "play",
    val json: PlayLogJson
)

@Serializable
data class PlayLogJson(
    val download: Int = 0,
    val end: String = "playend",
    val id: Long,
    val sourceId: Long,
    val time: Long,
    val type: String = "song",
    val wifi: Int = 0,
    val source: String = "list",
    val mainsite: String = "1",
    val mainsiteWeb: String = "1",
    val content: String
)
