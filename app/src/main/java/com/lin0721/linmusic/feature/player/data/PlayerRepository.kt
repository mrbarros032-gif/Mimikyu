package com.lin0721.linmusic.feature.player.data

import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.feature.player.domain.SongWikiData
import kotlinx.coroutines.flow.Flow

// PlayDetails（player ， PlayerViewModel）
interface PlayerRepository {

    fun getSongDetail(songId: Long): Flow<Result<Track>>

    // SongsDetails，1000 of ，
    fun getSongDetails(songIds: List<Long>): Flow<Result<List<Track>>>

    // of SongsDetails and
    fun getSongWiki(songId: Long): Flow<Result<SongWikiData>>

    // of （）， null
    fun getChorusStartTime(songId: Long): Flow<Result<Long?>>
}
