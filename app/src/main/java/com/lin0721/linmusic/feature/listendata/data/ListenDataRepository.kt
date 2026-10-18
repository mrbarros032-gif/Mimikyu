package com.lin0721.linmusic.feature.listendata.data

import com.lin0721.linmusic.feature.listendata.domain.ListenReport
import com.lin0721.linmusic.feature.listendata.domain.SongRank
import com.lin0721.linmusic.feature.listendata.domain.TodaySong
import com.lin0721.linmusic.feature.listendata.domain.YearStat
import kotlinx.coroutines.flow.Flow

// Listening Stats（feature/listendata）
interface ListenDataRepository {


    fun getTotalDuration(): Flow<Result<Long>>

    // Today's Plays
    fun getTodaySongs(): Flow<Result<List<TodaySong>>>

    // /Top Played，period  week or month
    fun getSongRank(period: String): Flow<Result<SongRank>>

    // /Listening Report
    fun getReport(period: String): Flow<Result<ListenReport>>

    // Yearly History
    fun getYearStats(): Flow<Result<List<YearStat>>>
}
