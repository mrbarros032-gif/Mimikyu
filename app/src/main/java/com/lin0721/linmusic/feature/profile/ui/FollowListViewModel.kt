package com.lin0721.linmusic.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.feature.profile.data.ProfileRepository
import com.lin0721.linmusic.feature.profile.domain.ProfileFollowUserItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

private const val PAGE_SIZE = 30

@Serializable
enum class FollowListMode {
    FOLLOWS,
    FOLLOWEDS
}

sealed interface FollowListUiState {
    data object Loading : FollowListUiState

    data class Success(
        val users: List<ProfileFollowUserItem>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : FollowListUiState

    data class Error(val message: String) : FollowListUiState
}

class FollowListViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    // ProfileViewModel：uid/mode ， koinViewModel()
    private var uid: Long = 0L
    private var mode: FollowListMode = FollowListMode.FOLLOWS

    private val _uiState = MutableStateFlow<FollowListUiState>(FollowListUiState.Loading)
    val uiState: StateFlow<FollowListUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private var offset = 0

    // FollowListScreen  of  LaunchedEffect(uid, mode)
    fun load(uid: Long, mode: FollowListMode) {
        this.uid = uid
        this.mode = mode
        load()
    }

    fun load() {
        _uiState.value = FollowListUiState.Loading
        offset = 0
        viewModelScope.launch {
            val flow = if (mode == FollowListMode.FOLLOWS) {
                profileRepository.getUserFollows(uid, offset = 0, limit = PAGE_SIZE)
            } else {
                profileRepository.getUserFolloweds(uid, offset = 0, limit = PAGE_SIZE)
            }

            flow.first()
                .onSuccess { page ->
                    offset = page.users.size
                    _uiState.value = FollowListUiState.Success(
                        users = page.users,
                        hasMore = page.hasMore,
                        isLoadingMore = false
                    )
                }
                .onFailure { error ->
                    val defaultMsg = if (mode == FollowListMode.FOLLOWS) "Failed to load following list" else "Failed to load followers list"
                    _uiState.value = FollowListUiState.Error(error.message ?: defaultMsg)
                }
        }
    }

    fun retry() {
        load()
    }

    fun loadMore() {
        val state = _uiState.value as? FollowListUiState.Success ?: return
        if (!state.hasMore || state.isLoadingMore) return
        _uiState.value = state.copy(isLoadingMore = true)
        viewModelScope.launch {
            val flow = if (mode == FollowListMode.FOLLOWS) {
                profileRepository.getUserFollows(uid, offset = offset, limit = PAGE_SIZE)
            } else {
                profileRepository.getUserFolloweds(uid, offset = offset, limit = PAGE_SIZE)
            }

            flow.first()
                .onSuccess { page ->
                    offset += page.users.size
                    val latest = _uiState.value as? FollowListUiState.Success ?: return@onSuccess
                    _uiState.value = latest.copy(
                        users = latest.users + page.users,
                        hasMore = page.hasMore,
                        isLoadingMore = false
                    )
                }
                .onFailure { error ->
                    _toastEvent.emit(error.message ?: "Failed to load more")
                    val latest = _uiState.value as? FollowListUiState.Success ?: return@onFailure
                    _uiState.value = latest.copy(isLoadingMore = false)
                }
        }
    }
}
