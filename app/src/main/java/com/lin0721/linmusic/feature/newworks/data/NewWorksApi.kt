package com.lin0721.linmusic.feature.newworks.data

import com.lin0721.linmusic.core.model.Track
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

// FollowArtists（New MVs / New Releases） of  Retrofit 。
interface NewWorksApi {

    // FollowArtists of New MVs，—— feed  MV，
    @POST("/weapi/sub/artist/new/works/mv/list")
    suspend fun getNewMvs(
        @Body body: NewWorksMvRequest = NewWorksMvRequest()
    ): NewWorksMvResponse

    // FollowArtists of New Releases（Tracks/Albums）， startTimestamp 。
    // ：album AlbumsAll， 157 ，
    // limit ， limit  of
    @POST("/eapi/sub/artist/new/works/song-mv/list/v2")
    suspend fun getNewReleases(
        @Body body: NewWorksReleaseRequest
    ): NewWorksReleaseResponse
}

// ======================= New MVs =======================

@Serializable
data class NewWorksMvRequest(
    val limit: Int = 20,
    val startTimestamp: Long = System.currentTimeMillis()
)

@Serializable
data class NewWorksMvResponse(
    val code: Int = 0,
    val data: NewWorksMvData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class NewWorksMvData(
    val hasMore: Boolean = false,
    val newWorks: List<NewWorksMvDto> = emptyList()
)

@Serializable
data class NewWorksMvDto(
    val mvId: Long = 0,
    val mvName: String = "",
    val mvCoverUrl: String = "",
    val duration: Long = 0,
    val playCount: Long = 0,
    val publishTime: Long = 0,
    val artistName: String = ""
)

// ======================= New Releases =======================

@Serializable
data class NewWorksReleaseRequest(
    val startTimestamp: Long = System.currentTimeMillis(),
    val sourceType: Int = 1,
    val limit: Int = 10,
    val firstRequest: Boolean = true
)

@Serializable
data class NewWorksReleaseResponse(
    val code: Int = 0,
    val data: NewWorksReleaseData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class NewWorksReleaseData(
    val hasMore: Boolean = false,
    val newWorks: List<NewWorksReleaseItem> = emptyList()
)

@Serializable
data class NewWorksReleaseItem(
    val publishTime: Long = 0,
    val info: NewWorksReleaseInfo = NewWorksReleaseInfo()
)

@Serializable
data class NewWorksReleaseInfo(
    val blockTitle: NewWorksBlockTitle = NewWorksBlockTitle(),
    val blockType: String = "",
    // AlbumsAll， firstOrNull() /Artists，
    val songLists: List<Track> = emptyList(),
    val albumSongCount: Int = 0
)

@Serializable
data class NewWorksBlockTitle(
    val artistName: String = "",
    val artistId: Long = 0,
    val imgUrl: String = "",
    val resourceId: Long = 0,
    val resourceName: String = "",
    val resourcePicUrl: String? = null
)
