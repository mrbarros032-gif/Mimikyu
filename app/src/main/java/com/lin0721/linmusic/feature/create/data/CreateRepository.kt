package com.lin0721.linmusic.feature.create.data

import com.lin0721.linmusic.core.model.PlaylistDetail
import kotlinx.coroutines.flow.Flow

// New Playlist（create ）
// create/artist/library/playlist ，
// manipulatePlaylistTracks CurrentSongsPlaylists，
// ， and
interface CreateRepository {

    // CreatePlaylists
    fun createPlaylist(name: String, privacy: Int = 0): Flow<Result<PlaylistDetail>>
}
