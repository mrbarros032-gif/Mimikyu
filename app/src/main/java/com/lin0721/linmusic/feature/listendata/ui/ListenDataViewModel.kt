package com.lin0721.linmusic.feature.listendata.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.R
import com.lin0721.linmusic.core.auth.AuthState
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.network.ResourceProvider
import com.lin0721.linmusic.core.network.toUserMessage
import com.lin0721.linmusic.core.player.PlayerManager
import com.lin0721.linmusic.core.player.QueueItem
import com.lin0721.linmusic.feature.listendata.data.ListenDataRepository
import com.lin0721.linmusic.feature.listendata.data.PERIOD_MONTH
import com.lin0721.linmusic.feature.listendata.data.PERIOD_WEEK
import com.lin0721.linmusic.feature.listendata.domain.ListenReport
import com.lin0721.linmusic.feature.listendata.domain.SongRank
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ListenDataViewModel"

class ListenDataViewModel(
    private val repository: ListenDataRepository,
    val playerManager: PlayerManager,
    private val resourceProvider: ResourceProvider,
    private val authState: AuthState
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListenDataUiState())
    val uiState: StateFlow<ListenDataUiState> = _uiState.asStateFlow()

    // of  Tab ，
    private val cache = mutableMapOf<ListenTab, ListenDataContent>()

    init {
        loadTotalDuration()
        selectTab(ListenTab.WEEK)
    }

    fun selectTab(tab: ListenTab) {
        val cached = cache[tab]
        _uiState.update { it.copy(selectedTab = tab, content = cached ?: ListenDataContent.Loading) }
        if (cached == null) loadTab(tab)
    }

    fun retry() {
        val tab = _uiState.value.selectedTab
        cache.remove(tab)
        _uiState.update { it.copy(content = ListenDataContent.Loading) }
        if (_uiState.value.totalSeconds == 0L) loadTotalDuration()
        loadTab(tab)
    }

    // Current Tab  of SongsQueue，
    fun playSongAt(index: Int) {
        val queue = when (val content = _uiState.value.content) {
            is ListenDataContent.Today -> content.songs.map {
                val vid = com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry.getVideoId(it.id).orEmpty()
                QueueItem(it.id, it.name, it.artistName, it.coverUrl, videoId = vid)
            }
            is ListenDataContent.Period -> content.rank?.songs.orEmpty().map {
                val vid = com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry.getVideoId(it.id).orEmpty()
                QueueItem(it.id, it.name, it.artistName, it.coverUrl, videoId = vid)
            }
            else -> emptyList()
        }
        if (index !in queue.indices) return
        playerManager.playQueue(queue, index, "Listening Stats")
    }

    // 。，
    fun playHighlight(songId: Long) {
        val content = _uiState.value.content as? ListenDataContent.Period ?: return
        val index = content.rank?.songs.orEmpty().indexOfFirst { it.id == songId }
        if (index >= 0) {
            playSongAt(index)
            return
        }
        val highlight = content.report?.highlights?.firstOrNull { it.songId == songId } ?: return
        playerManager.playQueue(
            listOf(QueueItem(highlight.songId, highlight.songName, "", highlight.coverUrl)),
            0,
            "Listening Stats"
        )
    }

    // Tab，
    private fun loadTotalDuration() {
        viewModelScope.launch {
            runCatching { repository.getTotalDuration().first() }
                .getOrNull()
                ?.onSuccess { seconds -> _uiState.update { it.copy(totalSeconds = seconds) } }
                ?.onFailure { AppLogger.w(TAG, "Total listening time failed to load", it) }
        }
    }

    private fun loadTab(tab: ListenTab) {
        viewModelScope.launch {
            val content = when (tab) {
                ListenTab.TODAY -> loadToday()
                ListenTab.WEEK -> loadPeriod(PERIOD_WEEK)
                ListenTab.MONTH -> loadPeriod(PERIOD_MONTH)
                ListenTab.YEAR -> loadYear()
            }
            // ，
            if (content !is ListenDataContent.Error) cache[tab] = content
            // ， Tab
            _uiState.update { if (it.selectedTab == tab) it.copy(content = content) else it }
        }
    }

    private suspend fun loadToday(): ListenDataContent =
        fetch("Today's Plays") { repository.getTodaySongs().first() }
            .fold(
                onSuccess = { ListenDataContent.Today(it) },
                onFailure = { ListenDataContent.Today(emptyList()) }
            )

    private suspend fun loadPeriod(period: String): ListenDataContent {
        val reportDeferred = viewModelScope.async { fetch("Listening Report") { repository.getReport(period).first() } }
        val rankDeferred = viewModelScope.async { fetch("Top Played") { repository.getSongRank(period).first() } }

        val report = reportDeferred.await()
        val rank = rankDeferred.await()

        return ListenDataContent.Period(
            report = report.getOrNull() ?: ListenReport(playMinutes = 0),
            rank = rank.getOrNull() ?: SongRank(totalCount = 0, songs = emptyList())
        )
    }

    private suspend fun loadYear(): ListenDataContent =
        fetch("Yearly History") { repository.getYearStats().first() }
            .fold(
                onSuccess = { ListenDataContent.Year(it) },
                onFailure = { ListenDataContent.Year(emptyList()) }
            )

    private suspend fun <T> fetch(label: String, block: suspend () -> Result<T>): Result<T> =
        runCatching { block() }
            .getOrElse { e ->
                AppLogger.w(TAG, "$label failed to load", e)
                Result.failure(e)
            }
            .onFailure { AppLogger.w(TAG, "$label Failed to load", it) }
}
