package com.lin0721.linmusic.feature.home.data

import com.lin0721.linmusic.feature.home.domain.HomeBlockPage
import com.lin0721.linmusic.feature.home.domain.ToplistInfo
import kotlinx.coroutines.flow.Flow

// （home ）
interface HomeRepository {

    // ，cursor
    fun getHomeBlockPage(refresh: Boolean = false, cursor: String = ""): Flow<Result<HomeBlockPage>>

    // RecommendedPlaylists（，）
    fun getPersonalizedPlaylists(): Flow<Result<PersonalizedData>>

    // Details（DTO ）
    fun getToplistDetail(): Flow<Result<List<ToplistInfo>>>

    // Daily RecommendationsSongs（）
    fun getDailyRecommendSongs(): Flow<Result<List<DailySong>>>

    // Daily History（VIP）
    fun getHistoryRecommendDates(): Flow<Result<List<String>>>

    // of Daily HistoryDetails
    fun getHistoryRecommendDetail(date: String): Flow<Result<List<DailySong>>>
}
