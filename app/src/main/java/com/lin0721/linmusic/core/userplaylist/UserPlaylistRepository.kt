package com.lin0721.linmusic.core.userplaylist

import kotlinx.coroutines.flow.Flow

// CurrentPlaylists（core ， library/artist/playlist  of "Save to Playlist"）
interface UserPlaylistRepository {

    fun getUserPlaylists(uid: Long, limit: Int = 1000): Flow<Result<List<UserPlaylist>>>
}
