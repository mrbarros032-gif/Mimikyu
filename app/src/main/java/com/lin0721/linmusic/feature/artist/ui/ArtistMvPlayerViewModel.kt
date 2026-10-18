package com.lin0721.linmusic.feature.artist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.core.auth.UserPreferences
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.model.ArtistMv
import com.lin0721.linmusic.core.network.ResourceProvider
import com.lin0721.linmusic.core.network.toUserMessage
import com.lin0721.linmusic.core.player.PlayerManager
import com.lin0721.linmusic.feature.artist.data.ArtistRepository
import com.lin0721.linmusic.feature.artist.domain.MvDetail
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "ArtistMvPlayerViewModel"

// MV Play UI ：Play，//Play Composable  of  ExoPlayer
sealed interface MvPlayerUiState {
    data object Loading : MvPlayerUiState
    data class Success(val videoUrl: String) : MvPlayerUiState
    data class Error(val message: String) : MvPlayerUiState
    // "Wi-Fi Only Playback"，
    data object Blocked : MvPlayerUiState
}

class ArtistMvPlayerViewModel(
    private val artistRepository: ArtistRepository,
    private val playerManager: PlayerManager,
    private val userPreferences: UserPreferences,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<MvPlayerUiState>(MvPlayerUiState.Loading)
    val uiState: StateFlow<MvPlayerUiState> = _uiState.asStateFlow()

    private val _mvDetail = MutableStateFlow<MvDetail?>(null)
    val mvDetail: StateFlow<MvDetail?> = _mvDetail.asStateFlow()



    private val _relatedMvs = MutableStateFlow<List<ArtistMv>>(emptyList())
    val relatedMvs: StateFlow<List<ArtistMv>> = _relatedMvs.asStateFlow()

    // ：Artists/Followers/Following（ArtistsDetails of ，）
    private val _artistAvatar = MutableStateFlow("")
    val artistAvatar: StateFlow<String> = _artistAvatar.asStateFlow()

    private val _fansCount = MutableStateFlow(0L)
    val fansCount: StateFlow<Long> = _fansCount.asStateFlow()

    private val _isArtistFollowed = MutableStateFlow(false)
    val isArtistFollowed: StateFlow<Boolean> = _isArtistFollowed.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    val userProfile = userPreferences.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // forcePlayOnMobile:  Blocked "Play Anyway"，
    fun loadMvUrl(mvId: Long, resolution: Int = 1080, forcePlayOnMobile: Boolean = false) {
        viewModelScope.launch {
            if (!forcePlayOnMobile && playerManager.shouldBlockPlaybackOnMobile()) {
                _uiState.value = MvPlayerUiState.Blocked
                return@launch
            }
            // MV PlayPause， MV
            if (playerManager.isPlaying.value) playerManager.pause()
            _uiState.value = MvPlayerUiState.Loading
            artistRepository.getMvUrl(mvId, resolution).collect { result ->
                result.onSuccess { url ->
                    _uiState.value = MvPlayerUiState.Success(url)
                }.onFailure { e ->
                    AppLogger.e(TAG, "Failed to fetch MV stream url: mvId=$mvId resolution=$resolution", e)
                    _uiState.value = MvPlayerUiState.Error(e.toUserMessage(resourceProvider))
                }
            }
        }
    }

    fun loadMvDetail(mvId: Long) {
        viewModelScope.launch {
            artistRepository.getMvDetail(mvId).first()
                .onSuccess { detail ->
                    _mvDetail.value = detail
                    loadRelatedMvs(detail.artistId, excludeMvId = mvId)
                    loadArtistInfo(detail.artistId)
                }
                .onFailure { e ->
                    AppLogger.w(TAG, "Failed to fetch MV details: mvId=$mvId", e)
                }
        }
    }

    private fun loadRelatedMvs(artistId: Long, excludeMvId: Long) {
        if (artistId <= 0) return
        viewModelScope.launch {
            artistRepository.getArtistMvs(artistId, limit = 10, offset = 0).first()
                .onSuccess { page ->
                    _relatedMvs.value = page.mvs.filter { it.id != excludeMvId }
                }
        }
    }

    // ：/Followers/Follow，AllArtistsDetails
    private fun loadArtistInfo(artistId: Long) {
        if (artistId <= 0) return
        viewModelScope.launch {
            val detailDeferred = async { artistRepository.getArtistDetail(artistId).first() }
            val fansDeferred = async { artistRepository.getArtistFansCount(artistId).first() }
            val followDeferred = async { artistRepository.checkArtistFollowed(artistId).first() }
            detailDeferred.await().onSuccess { _artistAvatar.value = it.avatar.ifBlank { it.cover } }
            fansDeferred.await().onSuccess { _fansCount.value = it }
            followDeferred.await().onSuccess { _isArtistFollowed.value = it }
        }
    }

    fun toggleArtistFollow() {
        viewModelScope.launch {
            if (userProfile.value == null) {
                _toastEvent.emit("Please sign in first")
                return@launch
            }
            val artistId = _mvDetail.value?.artistId ?: return@launch
            val target = !_isArtistFollowed.value
            _isArtistFollowed.value = target
            artistRepository.subscribeArtist(artistId, target).collect { result ->
                result.onSuccess {
                    _toastEvent.emit(if (target) "Following artist" else "Unfollowed artist")
                }.onFailure { e ->
                    _isArtistFollowed.value = !target
                    _toastEvent.emit(e.toUserMessage(resourceProvider))
                }
            }
        }
    }



    fun toggleLike(mvId: Long) {
        viewModelScope.launch {
            if (userProfile.value == null) {
                _toastEvent.emit("Please log in first")
                return@launch
            }
            val detail = _mvDetail.value ?: return@launch
            val target = !detail.isLiked
            _mvDetail.value = detail.copy(isLiked = target)
            artistRepository.likeMv(mvId, target).collect { result ->
                result.onFailure { e ->
                    _mvDetail.value = detail
                    _toastEvent.emit(e.toUserMessage(resourceProvider))
                }
            }
        }
    }

    fun toggleSubscribe(mvId: Long) {
        viewModelScope.launch {
            if (userProfile.value == null) {
                _toastEvent.emit("Please sign in first")
                return@launch
            }
            val detail = _mvDetail.value ?: return@launch
            val target = !detail.isSubscribed
            _mvDetail.value = detail.copy(isSubscribed = target)
            artistRepository.subscribeMv(mvId, target).collect { result ->
                result.onSuccess {
                    _toastEvent.emit(if (target) "Favorited MV" else "Unfavorited MV")
                }.onFailure { e ->
                    _mvDetail.value = detail
                    _toastEvent.emit(e.toUserMessage(resourceProvider))
                }
            }
        }
    }
}
