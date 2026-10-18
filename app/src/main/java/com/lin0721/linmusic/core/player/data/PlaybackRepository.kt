package com.lin0721.linmusic.core.player.data

import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.player.domain.LyricLine
import kotlinx.coroutines.flow.Flow

// Play（core ， PlayerManager/FloatingLyricService  of Recommended）
interface PlaybackRepository {

    // SongsPlay
    fun getSongUrl(songId: Long): Flow<Result<String>>

    // Songs（ LRC ）
    fun getLyrics(songId: Long): Flow<Result<List<LyricLine>>>

    fun getCachedLyrics(songId: Long): List<LyricLine>?

    fun getSimilarSongs(songId: Long): Flow<Result<List<Track>>>

    fun getIntelligenceSongs(songId: Long, playlistId: Long): Flow<Result<List<Track>>>

    // ，sourceId  songId ；Play，「Recently Played」
    fun reportStartPlay(songId: Long): Flow<Result<Unit>>

    // ：SongsPlay，「Listening Rank」
    fun reportPlayEnd(songId: Long, playedSeconds: Long): Flow<Result<Unit>>
}
