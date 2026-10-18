package com.lin0721.linmusic.feature.search.data

import com.lin0721.linmusic.core.model.EmptyBody
import com.lin0721.linmusic.feature.search.data.dto.CloudSearchRequest
import com.lin0721.linmusic.feature.search.data.dto.CloudSearchResponse
import com.lin0721.linmusic.feature.search.data.dto.HighQualityPlaylistRequest
import com.lin0721.linmusic.feature.search.data.dto.HighQualityPlaylistResponse
import com.lin0721.linmusic.feature.search.data.dto.HighQualityTagsResponse
import com.lin0721.linmusic.feature.search.data.dto.HotSearchDetailResponse
import com.lin0721.linmusic.feature.search.data.dto.SearchDefaultResponse
import com.lin0721.linmusic.feature.search.data.dto.SearchSuggestRequest
import com.lin0721.linmusic.feature.search.data.dto.SearchSuggestResponse
import com.lin0721.linmusic.feature.search.data.dto.SearchSuggestWebResponse
import retrofit2.http.Body
import retrofit2.http.POST

// Search of  Retrofit ，DTO  data/dto 。
interface SearchApi {

    // Search
    @POST("/eapi/search/defaultkeyword/get")
    suspend fun getSearchDefaultKeyword(
        @Body body: EmptyBody = EmptyBody()
    ): SearchDefaultResponse

    // Details
    @POST("/eapi/hotsearchlist/get")
    suspend fun getHotSearchDetail(
        @Body body: EmptyBody = EmptyBody()
    ): HotSearchDetailResponse

    // Search（ type Tracks/Albums/Artists/Playlists）
    @POST("/eapi/cloudsearch/pc")
    suspend fun cloudSearch(
        @Body body: CloudSearchRequest
    ): CloudSearchResponse

    // Curated Playlists
    @POST("/eapi/playlist/highquality/tags")
    suspend fun getHighQualityTags(
        @Body body: EmptyBody = EmptyBody()
    ): HighQualityTagsResponse

    // Curated Playlists（）
    @POST("/eapi/playlist/highquality/list")
    suspend fun getHighQualityPlaylists(
        @Body body: HighQualityPlaylistRequest
    ): HighQualityPlaylistResponse

    // Search（：weapi， of  eapi）
    @POST("/weapi/search/suggest/keyword")
    suspend fun getSearchSuggest(
        @Body body: SearchSuggestRequest
    ): SearchSuggestResponse

    // Search：Artists/Albums（：weapi）
    @POST("/weapi/search/suggest/web")
    suspend fun getSearchSuggestWeb(
        @Body body: SearchSuggestRequest
    ): SearchSuggestWebResponse
}
