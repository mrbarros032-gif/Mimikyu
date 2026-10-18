package com.lin0721.linmusic.feature.recent.data

import com.lin0721.linmusic.core.model.Artist
import com.lin0721.linmusic.core.model.PlaylistCreator
import com.lin0721.linmusic.core.model.Track
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

// Recently Played（Songs/Playlists/Albums） of  Retrofit 。
interface RecentApi {

    // Recently Played Tracks（）
    @POST("/weapi/play-record/song/list")
    suspend fun getRecentSongs(
        @Body body: RecentRecordRequest = RecentRecordRequest()
    ): RecentSongResponse

    // Recently Played Playlists（）
    @POST("/eapi/play-record/playlist/list")
    suspend fun getRecentPlaylists(
        @Body body: RecentRecordRequest = RecentRecordRequest()
    ): RecentPlaylistResponse

    // Recently Played Albums（）
    @POST("/weapi/play-record/album/list")
    suspend fun getRecentAlbums(
        @Body body: RecentRecordRequest = RecentRecordRequest()
    ): RecentAlbumResponse
}

// =======================  =======================

// limit ，， Repository
@Serializable
data class RecentRecordRequest(
    val limit: Int = 100
)

// ======================= Recently Played Tracks =======================

@Serializable
data class RecentSongResponse(
    val code: Int = 0,
    val data: RecentSongData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class RecentSongData(
    val list: List<RecentSongItem> = emptyList()
)

@Serializable
data class RecentSongItem(
    val playTime: Long = 0,
    val data: Track = Track()
)

// ======================= Recently Played Playlists =======================

@Serializable
data class RecentPlaylistResponse(
    val code: Int = 0,
    val data: RecentPlaylistData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class RecentPlaylistData(
    val list: List<RecentPlaylistItem> = emptyList()
)

@Serializable
data class RecentPlaylistItem(
    val playTime: Long = 0,
    val data: RecentPlaylistInfo = RecentPlaylistInfo()
)

@Serializable
data class RecentPlaylistInfo(
    val id: Long = 0,
    val name: String = "",
    @SerialName("coverImgUrl")
    val picUrl: String = "",
    val creator: PlaylistCreator? = null
)

// ======================= Recently Played Albums =======================

@Serializable
data class RecentAlbumResponse(
    val code: Int = 0,
    val data: RecentAlbumData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class RecentAlbumData(
    val list: List<RecentAlbumItem> = emptyList()
)

@Serializable
data class RecentAlbumItem(
    val playTime: Long = 0,
    val data: RecentAlbumInfo = RecentAlbumInfo()
)

// Albums（ 600KB），songs
@Serializable
data class RecentAlbumInfo(
    val id: Long = 0,
    val name: String = "",
    val picUrl: String = "",
    val artist: Artist? = null
)
