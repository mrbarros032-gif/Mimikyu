package com.lin0721.linmusic.feature.music.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.R
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.network.ResourceProvider
import com.lin0721.linmusic.core.network.toUserMessage
import com.lin0721.linmusic.core.player.PlayerManager
import com.lin0721.linmusic.core.player.QueueItem
import com.lin0721.linmusic.core.player.data.PlaybackRepository
import com.lin0721.linmusic.feature.music.data.MusicRepository
import com.lin0721.linmusic.feature.music.domain.StyleContent
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "MusicViewModel"

// 「」tab ViewModel：Genres and ，Genres
class MusicViewModel(
    private val musicRepository: MusicRepository,
    private val playbackRepository: PlaybackRepository,
    private val playerManager: PlayerManager,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<MusicUiState>(MusicUiState.Loading)
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // init ：Genres 140KB ，「All」 of 。
    // 「」tab  UI ，。
    fun loadIfNeeded() {
        if (_uiState.value is MusicUiState.Success) return
        loadStyles()
    }

    fun loadStyles() {
        _uiState.value = MusicUiState.Loading

        viewModelScope.launch {
            try {
                val stylesDeferred = async { musicRepository.getStyleList().first() }
                // ，，
                val preferencesDeferred = async {
                    runCatching { musicRepository.getStylePreferences().first() }
                        .getOrDefault(Result.success(emptyList()))
                }

                val stylesResult = stylesDeferred.await()
                val preferences = preferencesDeferred.await().getOrDefault(emptyList())
                val styles = stylesResult.getOrNull()

                if (styles.isNullOrEmpty()) {
                    _uiState.value = MusicUiState.Error(
                        stylesResult.exceptionOrNull()?.toUserMessage(resourceProvider)
                            ?: resourceProvider.getString(R.string.app_error_biz_default)
                    )
                    return@launch
                }

                // ，Genres
                val selection = if (preferences.isNotEmpty()) {
                    StyleSelection.Preference
                } else {
                    StyleSelection.Style(styles.first().id)
                }

                _uiState.value = MusicUiState.Success(
                    MusicFeedData(styles = styles, preferences = preferences, selection = selection)
                )
                loadContent()
            } catch (e: Exception) {
                _uiState.value = MusicUiState.Error(e.toUserMessage(resourceProvider))
            }
        }
    }

    fun selectStyle(selection: StyleSelection) {
        val current = _uiState.value as? MusicUiState.Success ?: return
        if (current.data.selection == selection && current.data.selectedChildId == null) return

        // Genres，Genres of
        _uiState.value = MusicUiState.Success(
            current.data.copy(selection = selection, selectedChildId = null, content = null)
        )
        loadContent()
    }

    fun selectChildStyle(childId: Long?) {
        val current = _uiState.value as? MusicUiState.Success ?: return
        if (current.data.selectedChildId == childId) return

        _uiState.value = MusicUiState.Success(current.data.copy(selectedChildId = childId, content = null))
        loadContent()
    }

    private fun loadContent() {
        val current = _uiState.value as? MusicUiState.Success ?: return
        val tagId = current.data.activeTagId ?: return

        _uiState.value = MusicUiState.Success(current.data.copy(isContentLoading = true))

        viewModelScope.launch {
            // ：，Genres
            val headDeferred = async { runCatching { musicRepository.getStyleHead(tagId).first() }.getOrNull()?.getOrNull() }
            val playlistsDeferred = async { runCatching { musicRepository.getStylePlaylists(tagId).first() }.getOrNull()?.getOrNull().orEmpty() }
            val songsDeferred = async { runCatching { musicRepository.getStyleSongs(tagId).first() }.getOrNull()?.getOrNull().orEmpty() }
            val artistsDeferred = async { runCatching { musicRepository.getStyleArtists(tagId).first() }.getOrNull()?.getOrNull().orEmpty() }

            val content = StyleContent(
                head = headDeferred.await(),
                playlists = playlistsDeferred.await(),
                songs = songsDeferred.await(),
                artists = artistsDeferred.await()
            )

            val latest = _uiState.value as? MusicUiState.Success ?: return@launch
            // Genres， of
            if (latest.data.activeTagId != tagId) {
                AppLogger.d(TAG, "Genre switched, discarding response for tagId=$tagId")
                return@launch
            }
            _uiState.value = MusicUiState.Success(latest.data.copy(content = content, isContentLoading = false))
        }
    }

    // GenresTracks，PlayQueue
    fun playSongAt(index: Int) {
        val current = _uiState.value as? MusicUiState.Success ?: return
        val songs = current.data.content?.songs.orEmpty()
        if (songs.isEmpty()) return

        val queue = songs.map { track ->
            QueueItem(track.id, track.name, track.ar.joinToString("/") { it.name }, track.al.picUrl)
        }
        val styleName = current.data.content?.head?.name ?: "Genres"
        playerManager.playQueue(queue, index.coerceIn(queue.indices), playContext = "style_$styleName")
    }

    // PlayGenres of 「Genres」
    fun playFavouriteSong(track: Track) {
        viewModelScope.launch {
            playbackRepository.getSongUrl(track.id).collect { result ->
                result.onSuccess { url ->
                    playerManager.playAudio(
                        track.id,
                        url,
                        track.name,
                        track.ar.joinToString("/") { it.name },
                        track.al.picUrl,
                        playContext = "style_favourite"
                    )
                }.onFailure { error ->
                    _toastEvent.emit(error.toUserMessage(resourceProvider))
                }
            }
        }
    }
}
