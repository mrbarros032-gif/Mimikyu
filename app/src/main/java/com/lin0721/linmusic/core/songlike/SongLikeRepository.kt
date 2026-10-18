package com.lin0721.linmusic.core.songlike

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// Songs（core ，Play and  player/playlist/artist ）
interface SongLikeRepository {

    val likedSongIds: StateFlow<Set<Long>>

    fun getLikedSongIds(uid: Long): Flow<Result<List<Long>>>

    fun likeSong(songId: Long, like: Boolean): Flow<Result<Unit>>

    fun syncLikedSongIds(ids: Set<Long>)
}
