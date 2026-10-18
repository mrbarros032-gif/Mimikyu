package com.lin0721.linmusic.feature.recent.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.R
import com.lin0721.linmusic.core.auth.AuthState
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.network.ResourceProvider
import com.lin0721.linmusic.core.network.toUserMessage
import com.lin0721.linmusic.core.player.PlayerManager
import com.lin0721.linmusic.core.player.QueueItem
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import com.lin0721.linmusic.feature.recent.data.RecentRepository
import com.lin0721.linmusic.feature.recent.domain.RecentSong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "RecentPlayViewModel"

class RecentPlayViewModel(
    private val recentRepository: RecentRepository,
    val playerManager: PlayerManager,
    private val resourceProvider: ResourceProvider,
    private val authState: AuthState
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecentPlayUiState>(RecentPlayUiState.Loading)
    val uiState: StateFlow<RecentPlayUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(RecentTab.SONG)
    val selectedTab: StateFlow<RecentTab> = _selectedTab.asStateFlow()

    init {
        load()
    }

    fun selectTab(tab: RecentTab) {
        _selectedTab.value = tab
    }

    fun load() {
        viewModelScope.launch {
            if (!authState.isLoggedIn) {
                _uiState.value = RecentPlayUiState.Error(
                    resourceProvider.getString(R.string.firebase_auth_signed_out)
                )
                return@launch
            }

            _uiState.value = RecentPlayUiState.Loading

            recentRepository.getRecentSongs().collect { result ->
                result.onSuccess { songs ->
                    _uiState.value = RecentPlayUiState.Success(
                        songs = songs,
                        playlists = emptyList(),
                        albums = emptyList()
                    )
                }.onFailure { error ->
                    _uiState.value = RecentPlayUiState.Error(
                        error.toUserMessage(resourceProvider)
                    )
                }
            }
        }
    }

    // Tapping a row plays via the normal YouTube Music path using videoId
    fun playSong(song: RecentSong) {
        val state = _uiState.value as? RecentPlayUiState.Success ?: return
        if (state.songs.isEmpty()) return

        val vid = song.videoId.ifBlank { YouTubeIdRegistry.getVideoId(song.track.id).orEmpty() }
        val item = QueueItem(
            songId = song.track.id,
            title = song.title,
            artist = song.artist,
            coverUrl = song.coverUrl,
            videoId = vid
        )
        playerManager.playWithDynamicRadio(item, "Listening History")
    }
}
