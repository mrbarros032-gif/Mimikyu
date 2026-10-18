package com.lin0721.linmusic.feature.playlist.data

import com.lin0721.linmusic.core.model.PlaylistDetail
import com.lin0721.linmusic.core.model.Track
import kotlinx.coroutines.flow.Flow

// Playlists/Albums（playlist ）
interface PlaylistRepository {

    // PlaylistsDetails
    fun getPlaylistDetail(id: Long): Flow<Result<PlaylistDetail>>

    // trackIds Playlists（ playlist/detail  of  tracks 1000）
    fun loadMoreTracks(trackIds: List<Long>): Flow<Result<List<Track>>>

    // AlbumsDetails， PlaylistDetail
    fun getAlbumDetail(id: Long): Flow<Result<PlaylistDetail>>

    // Save/CancelSavePlaylists
    fun subscribePlaylist(playlistId: Long, subscribe: Boolean): Flow<Result<Unit>>

    // Save/CancelSaveAlbums
    fun subscribeAlbum(albumId: Long, subscribe: Boolean): Flow<Result<Unit>>

    // PlaylistsSongsAdd/Delete，Songs ID
    fun manipulatePlaylistTracks(op: String, playlistId: Long, trackIds: List<Long>): Flow<Result<Unit>>

    // Playlists
    fun renamePlaylist(playlistId: Long, name: String): Flow<Result<Unit>>

    // Playlists
    fun updateDescription(playlistId: Long, desc: String): Flow<Result<Unit>>

    // Delete Playlist
    fun deletePlaylist(playlistId: Long): Flow<Result<Unit>>

    // Playlist Cover：coverImgId  NOS  of  ID，Back of
    fun updatePlaylistCover(playlistId: Long, coverImgId: Long): Flow<Result<String>>
}
