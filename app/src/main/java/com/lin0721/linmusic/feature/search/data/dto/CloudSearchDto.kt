package com.lin0721.linmusic.feature.search.data.dto

import com.lin0721.linmusic.core.model.Album
import com.lin0721.linmusic.core.model.Artist
import com.lin0721.linmusic.core.model.PlaylistDetail
import com.lin0721.linmusic.core.model.Track
import kotlinx.serialization.Serializable

@Serializable
data class CloudSearchRequest(
    val s: String,
    val type: Int = 1,      // 1=Tracks 10=Albums 100=Artists 1000=Playlists， SearchType
    val limit: Int = 30,
    val offset: Int = 0
)

@Serializable
data class CloudSearchResponse(
    val code: Int = 0,
    val result: SearchResult? = null
) {
    val isSuccess: Boolean get() = code == 200
}

// type  of ， type  of （）
@Serializable
data class SearchResult(
    val songs: List<Track>? = null,
    val songCount: Int = 0,
    val albums: List<Album>? = null,
    val albumCount: Int = 0,
    val artists: List<Artist>? = null,
    val artistCount: Int = 0,
    val playlists: List<PlaylistDetail>? = null,
    val playlistCount: Int = 0
)
