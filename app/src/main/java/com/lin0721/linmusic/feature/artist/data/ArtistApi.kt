package com.lin0721.linmusic.feature.artist.data

import com.lin0721.linmusic.core.model.Artist
import com.lin0721.linmusic.core.model.ArtistAlbum
import com.lin0721.linmusic.core.model.ArtistDetailInfo
import com.lin0721.linmusic.core.model.ArtistMv
import com.lin0721.linmusic.core.model.Track
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

// Artists/ of  Retrofit 。
interface ArtistApi {

    @POST("/eapi/discovery/simiArtist")
    suspend fun getSimiArtists(
        @Body body: SimiArtistRequest
    ): SimiArtistResponse

    @POST("/eapi/artist/head/info/get")
    suspend fun getArtistDetail(
        @Body body: ArtistDetailRequest
    ): ArtistDetailResponse

    // ArtistsFollowers and Follow
    @POST("/weapi/artist/follow/count/get")
    suspend fun getArtistFollowCount(
        @Body body: ArtistFollowCountRequest
    ): ArtistFollowCountGetResponse

    @POST("/weapi/artist/albums/{id}")
    suspend fun getArtistAlbums(
        @Path("id") id: Long,
        @Body body: ArtistAlbumRequest = ArtistAlbumRequest()
    ): ArtistAlbumResponse

    @POST("/weapi/artist/top/song")
    suspend fun getArtistTopSongs(
        @Body body: ArtistTopSongsRequest
    ): ArtistTopSongsResponse

    // ================== Save and CancelSaveArtists ==================
    @POST("/weapi/artist/{op}")
    suspend fun subscribeArtist(
        @Path("op") op: String, // sub Follow, unsub CancelFollow
        @Body body: ArtistSubscriptionRequest
    ): ArtistSubscriptionResponse

    // ================== Artists MV ==================
    @POST("/weapi/artist/mvs")
    suspend fun getArtistMvs(
        @Body body: ArtistMvsRequest
    ): ArtistMvsResponse

    // MV Play
    @POST("/weapi/song/enhance/play/mv/url")
    suspend fun getMvUrl(
        @Body body: MvUrlRequest
    ): MvUrlResponse

    // ================== ArtistsAllSongs（ api ，，eapi/weapi ）==================
    @POST("/eapi/v1/artist/songs")
    suspend fun getArtistAllSongsEapi(
        @Body body: ArtistAllSongsRequest
    ): ArtistAllSongsResponse

    @POST("/weapi/v1/artist/songs")
    suspend fun getArtistAllSongsWeapi(
        @Body body: ArtistAllSongsRequest
    ): ArtistAllSongsResponse

    // ================== MV Details/Save/（）==================
    @POST("/weapi/v1/mv/detail")
    suspend fun getMvDetail(
        @Body body: MvDetailRequest
    ): MvDetailResponse

    @POST("/weapi/mv/{op}")
    suspend fun subscribeMv(
        @Path("op") op: String, // sub Save, unsub CancelSave
        @Body body: MvSubscriptionRequest
    ): MvSubscriptionResponse

    // /Cancel（threadId  R_MV_5_{mvId}）
    @POST("/weapi/resource/{op}")
    suspend fun likeResource(
        @Path("op") op: String, // like , unlike Cancel
        @Body body: ResourceLikeRequest
    ): ResourceLikeResponse
}

// ======================= Artists DTO =======================

@Serializable
data class SimiArtistRequest(
    val artistid: Long
)

@Serializable
data class SimiArtistResponse(
    val code: Int = 0,
    val artists: List<Artist> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

// ======================= Details DTO =======================

@Serializable
data class ArtistDetailRequest(val id: Long)

@Serializable
data class ArtistDetailResponse(
    val code: Int = 0,
    val data: ArtistDetailData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class ArtistDetailData(
    val artist: ArtistDetailInfo? = null
)

// ======================= Followers DTO =======================

@Serializable
data class ArtistFollowCountRequest(val id: Long)

// ArtistsFollow and Followers
@Serializable
data class ArtistFollowCountData(
    @SerialName("fans") val fans: Long? = null,             // Followers
    @SerialName("fansCnt") val fansCnt: Long? = null,       // Followers (EAPIBack)
    @SerialName("fansCount") val fansCount: Long? = null,   // Followers
    @SerialName("followCount") val followCount: Long? = null, // Follow
    @SerialName("isFollow") val isFollow: Boolean = false    // CurrentFollowing
)

// ArtistsFollow ( concrete ， )
@Serializable
data class ArtistFollowCountGetResponse(
    @SerialName("code") val code: Int = 0,
    @SerialName("message") val message: String? = null, // Messages
    @SerialName("data") val data: ArtistFollowCountData? = null
) {

    val isSuccess: Boolean get() = code == 200
}

// ======================= Albums DTO =======================

@Serializable
data class ArtistAlbumRequest(
    val limit: Int = 10,
    val offset: Int = 0,
    val total: Boolean = true
)

@Serializable
data class ArtistAlbumResponse(
    val code: Int = 0,
    val hotAlbums: List<ArtistAlbum> = emptyList(),
    val more: Boolean = false
) {
    val isSuccess: Boolean get() = code == 200
}

// ======================= Tracks DTO =======================

@Serializable
data class ArtistTopSongsRequest(val id: Long)

@Serializable
data class ArtistTopSongsResponse(
    val code: Int = 0,
    val songs: List<Track> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

// ======================= Save and CancelSaveArtists DTO =======================

@Serializable
data class ArtistSubscriptionRequest(
    val artistId: Long,
    val artistIds: String
)

@Serializable
data class ArtistSubscriptionResponse(
    val code: Int = 0,
    val message: String? = null
) {
    val isSuccess: Boolean get() = code == 200
}

// ======================= Artists MV  DTO =======================

@Serializable
data class ArtistMvsRequest(
    val artistId: Long,
    val limit: Int = 20,
    val offset: Int = 0,
    val total: Boolean = true
)

@Serializable
data class ArtistMvsResponse(
    val code: Int = 0,
    val mvs: List<ArtistMv> = emptyList(),
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("hasMore", "more")
    val hasMore: Boolean = false
) {
    val isSuccess: Boolean get() = code == 200
}

// ======================= MV Play DTO =======================

@Serializable
data class MvUrlRequest(
    val id: Long,
    val r: Int = 1080
)

@Serializable
data class MvUrlResponse(
    val code: Int = 0,
    val data: MvUrlData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class MvUrlData(
    val id: Long = 0,
    val url: String? = null,
    val r: Int = 0,
    val size: Long = 0
)

// ======================= ArtistsAllSongs DTO =======================

@Serializable
data class ArtistAllSongsRequest(
    val id: Long,
    @SerialName("private_cloud") val privateCloud: String = "true",
    @SerialName("work_type") val workType: Int = 1,
    val order: String = "hot", // hot=, time=
    val offset: Int = 0,
    val limit: Int = 100
)

@Serializable
data class ArtistAllSongsResponse(
    val code: Int = 0,
    val songs: List<Track> = emptyList(),
    val more: Boolean = false,
    val total: Int = 0
) {
    val isSuccess: Boolean get() = code == 200
}

// ======================= MV Details DTO =======================
// ，；isSubed/isLiked  false，
// /Save，

@Serializable
data class MvDetailRequest(val id: Long)

@Serializable
data class MvDetailResponse(
    val code: Int = 0,
    val data: MvDetailData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class MvDetailData(
    val id: Long = 0,
    val name: String = "",
    val artistId: Long = 0,
    val artistName: String = "",
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("imgurl16v9", "imgurl", "cover")
    val cover: String = "",
    val duration: Long = 0,
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("playCount", "playcount")
    val playCount: Long = 0,
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("subCount", "subCnt")
    val subCount: Long = 0,
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("shareCount", "shareCnt")
    val shareCount: Long = 0,
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("commentCount", "commentCnt")
    val commentCount: Long = 0,
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("likedCount", "likedCnt")
    val likedCount: Long = 0,
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("subed", "hasSubed")
    val subed: Boolean = false,
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("liked", "hasLiked")
    val liked: Boolean = false,
    val publishTime: String = "",
    val briefDesc: String? = null
)

// ======================= Save/CancelSave MV DTO =======================

@Serializable
data class MvSubscriptionRequest(
    val mvId: Long,
    val mvIds: String
)

@Serializable
data class MvSubscriptionResponse(
    val code: Int = 0,
    val message: String? = null
) {
    val isSuccess: Boolean get() = code == 200
}

// =======================  DTO =======================

@Serializable
data class ResourceLikeRequest(
    val threadId: String
)

@Serializable
data class ResourceLikeResponse(
    val code: Int = 0,
    val message: String? = null
) {
    val isSuccess: Boolean get() = code == 200
}
