package com.lin0721.linmusic.core.songlike

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

// Songs of  Retrofit 。
interface SongLikeApi {

    // Like/UnlikeTracks（）
    @POST("/eapi/song/like")
    suspend fun likeSong(
        @Body body: LikeSongRequest
    ): LikeSongResponse

    // Current of Songs ID （）
    @POST("/eapi/song/like/get")
    suspend fun getLikedSongIds(
        @Body body: LikeSongListRequest
    ): LikeSongListResponse
}

@Serializable
data class LikeSongRequest(
    val trackId: Long,
    val like: Boolean = true
)

@Serializable
data class LikeSongResponse(
    val code: Int = 0,
    val playlistId: Long = 0
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class LikeSongListRequest(
    val uid: Long
)

@Serializable
data class LikeSongListResponse(
    val code: Int = 0,
    val ids: List<Long> = emptyList(),
    val checkPoint: Long = 0
) {
    val isSuccess: Boolean get() = code == 200
}
