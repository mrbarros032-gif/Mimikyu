package com.lin0721.linmusic.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.core.auth.AuthState
import com.lin0721.linmusic.core.auth.UserPreferences
import com.lin0721.linmusic.feature.profile.data.ProfileRepository
import com.lin0721.linmusic.feature.profile.domain.ProfileEventInfo
import com.lin0721.linmusic.feature.profile.domain.ProfileListenRankItem
import com.lin0721.linmusic.feature.profile.domain.ProfilePlaylistInfo
import com.lin0721.linmusic.feature.profile.domain.ProfileUserInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val PLAYLIST_PAGE_SIZE = 20
private const val EVENT_PAGE_SIZE = 20

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Success(
        val userInfo: ProfileUserInfo,
        val isSelf: Boolean,
        val selectedTab: Int = 0,
        val playlists: List<ProfilePlaylistInfo> = emptyList(),
        val playlistsHasMore: Boolean = false,
        val playlistsLoadingMore: Boolean = false,
        val playlistsLoaded: Boolean = false,
        val events: List<ProfileEventInfo> = emptyList(),
        val eventsHasMore: Boolean = false,
        val eventsLoadingMore: Boolean = false,
        val eventsLastTime: Long = -1L,
        val eventsLoaded: Boolean = false,
        val rankSubTab: Int = 0,
        val rankItems: List<ProfileListenRankItem> = emptyList(),
        val rankLoading: Boolean = false,
        val rankLoaded: Boolean = false
    ) : ProfileUiState

    data class Error(val message: String) : ProfileUiState
}

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val userPreferences: UserPreferences,
    private val userPlaylistRepository: com.lin0721.linmusic.core.userplaylist.UserPlaylistRepository? = null,
    private val authState: AuthState
) : ViewModel() {

    // Koin  of  koinViewModel() ， uid  of  ViewModel ；
    // uid ， LaunchedEffect(uid) ， of
    private var uid: Long = 0L

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private var playlistOffset = 0

    // ProfileScreen  of  LaunchedEffect(uid) ，uid
    fun load(uid: Long) {
        this.uid = uid
        loadUserProfile()
    }

    fun loadUserProfile() {
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            val authUser = authState.currentUser
            if (authUser == null) {
                _uiState.value = ProfileUiState.Error("Please sign in to view your profile")
                return@launch
            }

            val selfUserInfo = ProfileUserInfo(
                uid = 1L,
                nickname = authUser.displayName ?: "Music Lover",
                avatarUrl = authUser.photoUrl ?: "",
                signature = "Mimikyu Member",
                level = 8,
                followsCount = 0,
                followedsCount = 0,
                playlistCount = 0,
                isFollowedByMe = false
            )
            _uiState.value = ProfileUiState.Success(
                userInfo = selfUserInfo,
                isSelf = true
            )
            loadPlaylistsIfNeeded()
        }
    }

    fun retry() {
        loadUserProfile()
    }

    fun selectTab(tabIndex: Int) {
        val state = _uiState.value as? ProfileUiState.Success ?: return
        if (state.selectedTab == tabIndex) return
        _uiState.value = state.copy(selectedTab = tabIndex)
        when (tabIndex) {
            0 -> loadPlaylistsIfNeeded()
            1 -> loadListeningRankIfNeeded()
        }
    }

    fun loadPlaylistsIfNeeded() {
        val state = _uiState.value as? ProfileUiState.Success ?: return
        if (state.playlistsLoaded) return
        playlistOffset = 0
        // loading
        _uiState.value = state.copy(playlistsLoadingMore = true)
        viewModelScope.launch {
            if (userPlaylistRepository != null) {
                userPlaylistRepository.getUserPlaylists(state.userInfo.uid).first().onSuccess { list ->
                    val mapped = list.map { p ->
                        ProfilePlaylistInfo(
                            id = p.id,
                            name = p.name,
                            coverImgUrl = p.coverImgUrl,
                            playCount = p.playCount,
                            trackCount = p.trackCount,
                            creatorName = state.userInfo.nickname
                        )
                    }
                    val latest = _uiState.value as? ProfileUiState.Success ?: return@onSuccess
                    _uiState.value = latest.copy(
                        playlists = mapped,
                        playlistsHasMore = false,
                        playlistsLoaded = true,
                        playlistsLoadingMore = false
                    )
                }.onFailure {
                    val latest = _uiState.value as? ProfileUiState.Success ?: return@onFailure
                    _uiState.value = latest.copy(playlistsLoaded = true, playlistsLoadingMore = false)
                }
                return@launch
            }

            val latest = _uiState.value as? ProfileUiState.Success ?: return@launch
            _uiState.value = latest.copy(playlistsLoaded = true, playlistsLoadingMore = false)
        }
    }

    fun loadMorePlaylists() {
        val state = _uiState.value as? ProfileUiState.Success ?: return
        if (!state.playlistsHasMore || state.playlistsLoadingMore) return
        _uiState.value = state.copy(playlistsLoadingMore = true)
        viewModelScope.launch {
            profileRepository.getUserPlaylists(uid, offset = playlistOffset, limit = PLAYLIST_PAGE_SIZE)
                .first()
                .onSuccess { page ->
                    playlistOffset += page.playlists.size
                    val latest = _uiState.value as? ProfileUiState.Success ?: return@onSuccess
                    _uiState.value = latest.copy(
                        playlists = latest.playlists + page.playlists,
                        playlistsHasMore = page.hasMore,
                        playlistsLoadingMore = false
                    )
                }
                .onFailure { error ->
                    _toastEvent.emit(error.message ?: "Failed to load more playlists")
                    val latest = _uiState.value as? ProfileUiState.Success ?: return@onFailure
                    _uiState.value = latest.copy(playlistsLoadingMore = false)
                }
        }
    }

    fun loadEventsIfNeeded() {
        // Moments removed
    }

    fun loadMoreEvents() {
        // Moments removed
    }

    fun selectRankSubTab(subTab: Int) {
        val state = _uiState.value as? ProfileUiState.Success ?: return
        if (state.rankSubTab == subTab && state.rankLoaded) return
        _uiState.value = state.copy(rankSubTab = subTab)
        loadListeningRank(subTab)
    }

    fun loadListeningRankIfNeeded() {
        val state = _uiState.value as? ProfileUiState.Success ?: return
        if (state.rankLoaded) return
        loadListeningRank(state.rankSubTab)
    }

    private fun loadListeningRank(type: Int) {
        val state = _uiState.value as? ProfileUiState.Success ?: return
        _uiState.value = state.copy(rankLoading = true)
        viewModelScope.launch {
            profileRepository.getUserListeningRank(uid, type = type)
                .first()
                .onSuccess { items ->
                    val latest = _uiState.value as? ProfileUiState.Success ?: return@onSuccess
                    _uiState.value = latest.copy(
                        rankItems = items,
                        rankLoading = false,
                        rankLoaded = true
                    )
                }
                .onFailure { error ->
                    _toastEvent.emit(error.message ?: "Failed to load listening ranking")
                    val latest = _uiState.value as? ProfileUiState.Success ?: return@onFailure
                    _uiState.value = latest.copy(
                        rankLoading = false,
                        rankLoaded = true
                    )
                }
        }
    }

    fun toggleFollow() {
        val state = _uiState.value as? ProfileUiState.Success ?: return
        val currentFollowed = state.userInfo.isFollowedByMe
        val targetFollowed = !currentFollowed
        viewModelScope.launch {
            val request = if (targetFollowed) {
                profileRepository.followUser(uid)
            } else {
                profileRepository.unfollowUser(uid)
            }

            request.first()
                .onSuccess {
                    val latest = _uiState.value as? ProfileUiState.Success ?: return@onSuccess
                    val newFollowedsCount = if (targetFollowed) {
                        latest.userInfo.followedsCount + 1
                    } else {
                        (latest.userInfo.followedsCount - 1).coerceAtLeast(0)
                    }
                    val updatedUserInfo = latest.userInfo.copy(
                        isFollowedByMe = targetFollowed,
                        followedsCount = newFollowedsCount
                    )
                    _uiState.value = latest.copy(userInfo = updatedUserInfo)
                    val msg = if (targetFollowed) "Followed successfully" else "Unfollowed"
                    _toastEvent.emit(msg)
                }
                .onFailure { error ->
                    val defaultMsg = if (targetFollowed) "Failed to follow" else "Failed to unfollow"
                    _toastEvent.emit(error.message ?: defaultMsg)
                }
        }
    }
}
