package com.lin0721.linmusic.feature.music.data

import com.lin0721.linmusic.core.model.EmptyBody
import retrofit2.http.Body
import retrofit2.http.POST

// 「」tab  of Genres。 eapi/weapi ， eapi  and 。
interface MusicApi {

    // Genres：28 Genres，（）
    @POST("/eapi/tag/list/get")
    suspend fun getStyleList(
        @Body body: EmptyBody = EmptyBody()
    ): StyleListResponse

    // of Genres，（，Back）
    @POST("/eapi/tag/my/preference/get")
    suspend fun getStylePreference(
        @Body body: EmptyBody = EmptyBody()
    ): StylePreferenceResponse

    // GenresDetails：、、、Genres、Genres of
    @POST("/eapi/style-tag/home/head")
    suspend fun getStyleHead(
        @Body body: StyleHeadRequest
    ): StyleHeadResponse

    // GenresPlaylists
    @POST("/eapi/style-tag/home/playlist")
    suspend fun getStylePlaylists(
        @Body body: StyleContentRequest
    ): StylePlaylistResponse

    // GenresSongs
    @POST("/eapi/style-tag/home/song")
    suspend fun getStyleSongs(
        @Body body: StyleContentRequest
    ): StyleSongResponse

    // GenresArtists
    @POST("/eapi/style-tag/home/artist")
    suspend fun getStyleArtists(
        @Body body: StyleContentRequest
    ): StyleArtistResponse
}
