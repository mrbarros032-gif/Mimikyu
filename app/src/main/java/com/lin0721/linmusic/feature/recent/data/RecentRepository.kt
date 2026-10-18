package com.lin0721.linmusic.feature.recent.data

import com.lin0721.linmusic.feature.recent.domain.RecentAlbum
import com.lin0721.linmusic.feature.recent.domain.RecentPlaylist
import com.lin0721.linmusic.feature.recent.domain.RecentSong
import kotlinx.coroutines.flow.Flow

// Recently Played（feature/recent），Recently Played and
interface RecentRepository {

    // Recently Played Tracks
    fun getRecentSongs(): Flow<Result<List<RecentSong>>>

    // Recently Played Playlists
    fun getRecentPlaylists(): Flow<Result<List<RecentPlaylist>>>

    // Recently Played Albums
    fun getRecentAlbums(): Flow<Result<List<RecentAlbum>>>
}
