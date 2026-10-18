package com.lin0721.linmusic.feature.profile.data

import com.lin0721.linmusic.core.model.EmptyBody
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface ProfileApi {

    // Details (EAPI)
    @POST("/eapi/w/v1/user/detail/{uid}")
    suspend fun getUserDetailEapi(
        @Path("uid") uid: Long,
        @Body body: EmptyBody = EmptyBody()
    ): ProfileUserDetailResponse

    // Details (WEAPI)
    @POST("/weapi/v1/user/detail/{uid}")
    suspend fun getUserDetailWeapi(
        @Path("uid") uid: Long,
        @Body body: EmptyBody = EmptyBody()
    ): ProfileUserDetailResponse

    // Follow
    @POST("/weapi/user/follow/{id}")
    suspend fun followUser(
        @Path("id") uid: Long,
        @Body body: EmptyBody = EmptyBody()
    ): SimpleActionResponse

    // CancelFollow
    @POST("/weapi/user/delfollow/{id}")
    suspend fun unfollowUser(
        @Path("id") uid: Long,
        @Body body: EmptyBody = EmptyBody()
    ): SimpleActionResponse

    // Playlists
    @POST("/weapi/user/playlist")
    suspend fun getUserPlaylists(
        @Body body: ProfileUserPlaylistsRequest
    ): ProfileUserPlaylistsResponse

    // Follow
    @POST("/weapi/user/getfollows/{uid}")
    suspend fun getUserFollows(
        @Path("uid") uid: Long,
        @Body body: ProfileUserFollowsRequest
    ): ProfileFollowListResponse

    // Followers
    @POST("/weapi/user/getfolloweds/{uid}")
    suspend fun getUserFolloweds(
        @Path("uid") uid: Long,
        @Body body: ProfileUserFollowedsRequest
    ): ProfileFollowListResponse

    // Moments
    @POST("/weapi/event/get/{uid}")
    suspend fun getUserEvents(
        @Path("uid") uid: Long,
        @Body body: ProfileUserEventsRequest
    ): ProfileEventResponse

    // Listening Rank
    @POST("/weapi/v1/play/record")
    suspend fun getUserListeningRank(
        @Body body: ProfileListeningRankRequest
    ): ProfileListeningRankResponse
}

// ======================= Details DTO =======================

@Serializable
data class ProfileUserDetailResponse(
    val level: Int = 0,
    val listenSongs: Int = 0,
    val profile: ProfileUserBasic = ProfileUserBasic()
) {
    val isSuccess: Boolean
        get() = profile.userId > 0L
}

@Serializable
data class ProfileUserBasic(
    val userId: Long = 0L,
    val nickname: String = "",
    val avatarUrl: String = "",
    val signature: String = "",
    val gender: Int = 0,
    val follows: Int = 0,
    val followeds: Int = 0,
    val playlistCount: Int = 0,
    val followed: Boolean = false // Following
)

// =======================  DTO =======================

@Serializable
data class SimpleActionResponse(
    val code: Int
) {
    val isSuccess: Boolean
        get() = code == 200
}

// ======================= Playlists DTO =======================

@Serializable
data class ProfileUserPlaylistsRequest(
    val uid: Long,
    val limit: Int = 30,
    val offset: Int = 0,
    val includeVideo: Boolean = true
)

@Serializable
data class ProfileUserPlaylistsResponse(
    val code: Int,
    val playlist: List<ProfilePlaylist> = emptyList(),
    val more: Boolean = false
) {
    val isSuccess: Boolean
        get() = code == 200
}

@Serializable
data class ProfilePlaylist(
    val id: Long = 0L,
    val name: String = "",
    val coverImgUrl: String = "",
    val playCount: Long = 0L,
    val trackCount: Int = 0,
    val creator: ProfileUserBasic? = null
)

// ======================= Follow/Followers DTO =======================

@Serializable
data class ProfileUserFollowsRequest(
    val offset: Int = 0,
    val limit: Int = 30,
    val order: Boolean = true
)

@Serializable
data class ProfileUserFollowedsRequest(
    val userId: Long,
    val time: String = "0",
    val limit: Int = 20,
    val offset: Int = 0,
    val getcounts: String = "true"
)

@Serializable
data class ProfileFollowListResponse(
    val code: Int,
    val follow: List<ProfileUserBasic> = emptyList(),
    val followeds: List<ProfileUserBasic> = emptyList(), // FollowFollowers，
    val more: Boolean = false
) {
    val isSuccess: Boolean
        get() = code == 200
}

// ======================= Moments DTO =======================

@Serializable
data class ProfileUserEventsRequest(
    val getcounts: Boolean = true,
    val time: Long = -1L,
    val limit: Int = 30,
    val total: Boolean = false,
    val fromRN: String = "true"
)

@Serializable
data class ProfileEventResponse(
    val code: Int,
    val events: List<ProfileEvent> = emptyList(),
    val lasttime: Long = -1L,
    val more: Boolean = false
) {
    val isSuccess: Boolean
        get() = code == 200
}

@Serializable
data class ProfileEvent(
    val id: Long = 0L,
    // Moments json string，
    val json: String = "",
    val showTime: Long = 0L,
    val user: ProfileUserBasic? = null
)

// ======================= Listening Rank DTO =======================

@Serializable
data class ProfileListeningRankRequest(
    val uid: Long,
    val type: Int = 0
)

@Serializable
data class ProfileListeningRankResponse(
    val code: Int,
    val weekData: List<ProfileListenRecord> = emptyList(),
    val allData: List<ProfileListenRecord> = emptyList()
) {
    val isSuccess: Boolean
        get() = code == 200
}

@Serializable
data class ProfileListenRecord(
    val playCount: Int = 0,
    val score: Int = 0,
    val song: ProfileSong? = null
)

@Serializable
data class ProfileSong(
    val id: Long = 0L,
    val name: String = "",
    val ar: List<ProfileArtist> = emptyList(),
    val al: ProfileAlbum? = null
)

@Serializable
data class ProfileArtist(
    val id: Long = 0L,
    val name: String = ""
)

@Serializable
data class ProfileAlbum(
    val id: Long = 0L,
    val name: String = "",
    val picUrl: String = ""
)
