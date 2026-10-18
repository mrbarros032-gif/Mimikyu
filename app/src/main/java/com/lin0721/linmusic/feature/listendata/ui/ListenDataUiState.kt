package com.lin0721.linmusic.feature.listendata.ui

import com.lin0721.linmusic.feature.listendata.domain.ListenReport
import com.lin0721.linmusic.feature.listendata.domain.SongRank
import com.lin0721.linmusic.feature.listendata.domain.TodaySong
import com.lin0721.linmusic.feature.listendata.domain.YearStat

// 。 and  of ，
enum class ListenTab(val label: String) {
    TODAY("Today"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year")
}

// Current Tab  of 。 Tab ，
sealed interface ListenDataContent {
    data object Loading : ListenDataContent

    data class Today(val songs: List<TodaySong>) : ListenDataContent

    // and ， null，
    data class Period(
        val report: ListenReport?,
        val rank: SongRank?
    ) : ListenDataContent

    data class Year(val stats: List<YearStat>) : ListenDataContent

    data class Error(val message: String) : ListenDataContent
}

data class ListenDataUiState(
    val totalSeconds: Long = 0,
    val selectedTab: ListenTab = ListenTab.WEEK,
    val content: ListenDataContent = ListenDataContent.Loading
)
