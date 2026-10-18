package com.lin0721.linmusic.feature.create.data

import com.lin0721.linmusic.core.model.PlaylistDetail
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

// New Playlist of  Retrofit 。
interface CreateApi {

    // CreatePlaylists
    @POST("/eapi/playlist/create")
    suspend fun createPlaylist(
        @Body body: PlaylistCreateRequest
    ): PlaylistCreateResponse
}

@Serializable
data class PlaylistCreateRequest(
    val name: String,
    val privacy: Int = 0,
    val type: String = "NORMAL"
)

@Serializable
data class PlaylistCreateResponse(
    val code: Int = 0,
    val id: Long = 0,
    val playlist: PlaylistDetail? = null
) {
    val isSuccess: Boolean get() = code == 200
}
