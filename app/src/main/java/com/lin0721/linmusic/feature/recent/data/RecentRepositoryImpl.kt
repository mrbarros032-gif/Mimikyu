package com.lin0721.linmusic.feature.recent.data

import com.lin0721.linmusic.core.history.PlaybackHistoryRepository
import com.lin0721.linmusic.feature.recent.domain.RecentAlbum
import com.lin0721.linmusic.feature.recent.domain.RecentPlaylist
import com.lin0721.linmusic.feature.recent.domain.RecentSong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class RecentRepositoryImpl(
    private val playbackHistoryRepository: PlaybackHistoryRepository
) : RecentRepository {

    override fun getRecentSongs(): Flow<Result<List<RecentSong>>> =
        playbackHistoryRepository.plays.map { Result.success(it) }

    override fun getRecentPlaylists(): Flow<Result<List<RecentPlaylist>>> = flow {
        emit(Result.success(emptyList()))
    }

    override fun getRecentAlbums(): Flow<Result<List<RecentAlbum>>> = flow {
        emit(Result.success(emptyList()))
    }
}
