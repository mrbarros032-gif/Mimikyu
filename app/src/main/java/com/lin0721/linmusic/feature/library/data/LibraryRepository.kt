package com.lin0721.linmusic.feature.library.data

import com.lin0721.linmusic.core.model.Track
import kotlinx.coroutines.flow.Flow

// Listening RankTracks：Play，
data class UserRecordTrack(
    val track: Track,
    val playCount: Int
)

// （library ）
interface LibraryRepository {

    // Listening Rank
    fun getUserRecord(uid: Long, type: Int): Flow<Result<List<UserRecordTrack>>>

    // SaveAlbums
    fun getCollectedAlbums(limit: Int = 1000): Flow<Result<List<AlbumSubItem>>>

    // Save
    fun getUserSubcount(): Flow<Result<UserSubcountResponse>>
}
