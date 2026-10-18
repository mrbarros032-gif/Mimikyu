package com.lin0721.linmusic.feature.artist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.core.auth.UserPreferences
import com.lin0721.linmusic.core.model.ArtistDetailInfo
import com.lin0721.linmusic.core.model.ArtistAlbum
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.model.ArtistInfo
import com.lin0721.linmusic.core.auth.SyncProfileAfterLoginUseCase
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.songlike.LoadLikedSongIdsUseCase
import com.lin0721.linmusic.core.songlike.SongLikeRepository
import com.lin0721.linmusic.feature.artist.data.ArtistRepository
import com.lin0721.linmusic.feature.playlist.domain.SongCollectDelegate
import com.lin0721.linmusic.core.player.PlayerManager
import com.lin0721.linmusic.core.player.QueueItem
import com.lin0721.linmusic.core.ui.components.PlaylistCollectState
import com.lin0721.linmusic.core.ui.components.PlaylistCollectItem
import com.lin0721.linmusic.core.network.ResourceProvider
import com.lin0721.linmusic.core.network.toUserMessage
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val TAG = "ArtistViewModel"


private const val ALBUMS_PAGE_SIZE = 50
private const val MVS_PAGE_SIZE = 20
private const val ALL_SONGS_PAGE_SIZE = 50

class ArtistViewModel(
    private val songCollectDelegate: SongCollectDelegate,
    private val syncProfileAfterLoginUseCase: SyncProfileAfterLoginUseCase,
    private val loadLikedSongIdsUseCase: LoadLikedSongIdsUseCase,
    private val artistRepository: ArtistRepository,
    private val songLikeRepository: SongLikeRepository,
    val playerManager: PlayerManager,
    private val userPreferences: UserPreferences,
    private val resourceProvider: ResourceProvider,
    private val lastFmRepository: com.lin0721.linmusic.core.lastfm.LastFmRepository,
    private val mimikyuArtistStatsRepository: com.lin0721.linmusic.core.metadata.MimikyuArtistStatsRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ArtistUiState>(ArtistUiState.Loading)
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    val userProfile = userPreferences.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val blockedArtistIds = userPreferences.blockedArtistIds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    fun toggleBlockArtist(artistId: Long) {
        viewModelScope.launch {
            userPreferences.toggleBlockArtist(artistId)
            val isBlocked = userPreferences.blockedArtistIds.first().contains(artistId)
            val msg = if (isBlocked) "Blocked all songs from this artist" else "Unblocked all songs from this artist"
            _toastEvent.emit(msg)
        }
    }

    val likedSongIds: StateFlow<Set<Long>> = songLikeRepository.likedSongIds

    val collectState: StateFlow<PlaylistCollectState> = songCollectDelegate.state

    // Current of ， loadArtistData Reset
    private var currentArtistId: Long = 0
    private var albumOffset = 0
    private var mvOffset = 0
    private var allSongsOffset = 0

    init {
        loadLikedSongIds()
    }

    fun loadLikedSongIds() {
        viewModelScope.launch {
            loadLikedSongIdsUseCase()
        }
    }

    fun loadArtistData(artistId: Long) {
        currentArtistId = artistId
        albumOffset = 0
        mvOffset = 0
        allSongsOffset = 0
        _uiState.value = ArtistUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // of
                val detailDeferred = async { artistRepository.getArtistDetail(artistId).first() }
                val fansDeferred = async { artistRepository.getArtistFansCount(artistId).first() }
                val followDeferred = async { artistRepository.checkArtistFollowed(artistId).first() }
                val topSongsDeferred = async { artistRepository.getArtistTopSongs(artistId).first() }
                val albumsDeferred = async { artistRepository.getArtistAlbums(artistId, limit = ALBUMS_PAGE_SIZE, offset = 0).first() }
                val similarDeferred = async { artistRepository.getSimilarArtists(artistId).first() }
                val mvsDeferred = async { artistRepository.getArtistMvs(artistId, limit = MVS_PAGE_SIZE, offset = 0).first() }

                val detailResult = detailDeferred.await()
                val fansResult = fansDeferred.await()
                val followResult = followDeferred.await()
                val topSongsResult = topSongsDeferred.await()
                val albumsResult = albumsDeferred.await()
                val similarResult = similarDeferred.await()
                val mvsResult = mvsDeferred.await()

                if (detailResult.isSuccess || topSongsResult.isSuccess) {
                    val fallbackName = YouTubeIdRegistry.getArtistName(artistId).ifBlank { "Artist" }
                    var detail = detailResult.getOrNull() ?: ArtistDetailInfo(
                        id = artistId,
                        name = fallbackName,
                        cover = "",
                        avatar = "",
                        briefDesc = "",
                        albumSize = 0,
                        musicSize = 0,
                        identifyTag = emptyList()
                    )
                    var fans = fansResult.getOrDefault(0L)
                    var playCount = 0L
                    var mimikyuMonthly = 0L

                    // Last.fm authentic listener and scrobble data
                    if (lastFmRepository != null && detail.name.isNotBlank()) {
                        val lastFmInfo = lastFmRepository.getArtistInfo(detail.name)
                        if (lastFmInfo != null) {
                            if (lastFmInfo.listeners > 0L) {
                                fans = lastFmInfo.listeners
                            }
                            playCount = lastFmInfo.playcount
                            if (detail.briefDesc.isBlank() && lastFmInfo.bioSummary.isNotBlank()) {
                                detail = detail.copy(briefDesc = lastFmInfo.bioSummary)
                            }
                        }
                    }

                    // Authentic Mimikyu Monthly Listeners from Kworb dataset
                    val artistLookupName = detail.name.ifBlank { fallbackName }
                    val mimikyuStats = mimikyuArtistStatsRepository?.getStats(artistLookupName)
                    if (mimikyuStats != null && mimikyuStats.monthlyListeners > 0L) {
                        mimikyuMonthly = mimikyuStats.monthlyListeners
                        // If Last.fm had 0 or fallback listeners, use Mimikyu monthly listeners
                        if (fans == 0L) {
                            fans = mimikyuStats.monthlyListeners
                        }
                    } else if (artistLookupName.isNotBlank()) {
                        // Fallback to Audiometa API if not in the top 2,400 dataset
                        runCatching {
                            val audiometaInfo = com.lin0721.linmusic.core.audiometa.AudiometaRepository().getArtistInfo(artistLookupName)
                            val metaMonthly = audiometaInfo?.mimikyuMonthlyListeners
                            if (metaMonthly != null && metaMonthly > 0L) {
                                mimikyuMonthly = metaMonthly
                                if (fans == 0L) fans = metaMonthly
                            }
                        }
                    }

                    val isFollowed = followResult.getOrDefault(false)
                    val topSongs = topSongsResult.getOrDefault(emptyList())
                    val albumsPage = albumsResult.getOrNull()
                    val similar = similarResult.getOrDefault(emptyList())
                    val mvsPage = mvsResult.getOrNull()

                    albumOffset = albumsPage?.albums?.size ?: 0
                    mvOffset = mvsPage?.mvs?.size ?: 0

                    _uiState.value = ArtistUiState.Success(
                        artist = detail,
                        isFollowed = isFollowed,
                        fansCount = fans,
                        playCount = playCount,
                        mimikyuMonthlyListeners = mimikyuMonthly,
                        topSongs = topSongs,
                        albums = albumsPage?.albums ?: emptyList(),
                        albumsHasMore = albumsPage?.hasMore ?: false,
                        similarArtists = similar,
                        mvs = mvsPage?.mvs ?: emptyList(),
                        mvsHasMore = mvsPage?.hasMore ?: false
                    )
                } else {
                    val err = (detailResult.exceptionOrNull() ?: topSongsResult.exceptionOrNull())
                        ?.toUserMessage(resourceProvider)
                        ?: resourceProvider.getString(com.lin0721.linmusic.R.string.app_error_biz_default)
                    _uiState.value = ArtistUiState.Error(err)
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Artist details page failed to load: artistId=$artistId", e)
                _uiState.value = ArtistUiState.Error(e.toUserMessage(resourceProvider))
            }
        }
    }

    // Albums Tab
    fun loadMoreAlbums() {
        val state = _uiState.value as? ArtistUiState.Success ?: return
        if (!state.albumsHasMore || state.albumsLoadingMore) return
        _uiState.value = state.copy(albumsLoadingMore = true)
        viewModelScope.launch(Dispatchers.IO) {
            artistRepository.getArtistAlbums(currentArtistId, limit = ALBUMS_PAGE_SIZE, offset = albumOffset)
                .first()
                .onSuccess { page ->
                    albumOffset += page.albums.size
                    val latest = _uiState.value as? ArtistUiState.Success ?: return@onSuccess
                    _uiState.value = latest.copy(
                        albums = latest.albums + page.albums,
                        albumsHasMore = page.hasMore,
                        albumsLoadingMore = false
                    )
                }
                .onFailure { e ->
                    AppLogger.w(TAG, "Failed to load more albums: artistId=$currentArtistId", e)
                    val latest = _uiState.value as? ArtistUiState.Success ?: return@onFailure
                    _uiState.value = latest.copy(albumsLoadingMore = false)
                }
        }
    }

    // MV Tab
    fun loadMoreMvs() {
        val state = _uiState.value as? ArtistUiState.Success ?: return
        if (!state.mvsHasMore || state.mvsLoadingMore) return
        _uiState.value = state.copy(mvsLoadingMore = true)
        viewModelScope.launch(Dispatchers.IO) {
            artistRepository.getArtistMvs(currentArtistId, limit = MVS_PAGE_SIZE, offset = mvOffset)
                .first()
                .onSuccess { page ->
                    mvOffset += page.mvs.size
                    val latest = _uiState.value as? ArtistUiState.Success ?: return@onSuccess
                    _uiState.value = latest.copy(
                        mvs = latest.mvs + page.mvs,
                        mvsHasMore = page.hasMore,
                        mvsLoadingMore = false
                    )
                }
                .onFailure { e ->
                    AppLogger.w(TAG, "Failed to load more MVs: artistId=$currentArtistId", e)
                    val latest = _uiState.value as? ArtistUiState.Success ?: return@onFailure
                    _uiState.value = latest.copy(mvsLoadingMore = false)
                }
        }
    }

    // 「」Tab 「All」 Tab ，
    fun loadAllSongsIfNeeded() {
        val state = _uiState.value as? ArtistUiState.Success ?: return
        if (state.allSongsLoaded || state.allSongsLoadingMore) return
        _uiState.value = state.copy(allSongsLoadingMore = true)
        viewModelScope.launch(Dispatchers.IO) {
            artistRepository.getArtistAllSongs(currentArtistId, offset = 0, limit = ALL_SONGS_PAGE_SIZE)
                .first()
                .onSuccess { page ->
                    allSongsOffset = page.songs.size
                    val latest = _uiState.value as? ArtistUiState.Success ?: return@onSuccess
                    _uiState.value = latest.copy(
                        allSongs = page.songs,
                        allSongsHasMore = page.hasMore,
                        allSongsLoadingMore = false,
                        allSongsLoaded = true
                    )
                }
                .onFailure { e ->
                    AppLogger.w(TAG, "Failed to load all songs: artistId=$currentArtistId", e)
                    val latest = _uiState.value as? ArtistUiState.Success ?: return@onFailure
                    _uiState.value = latest.copy(allSongsLoadingMore = false, allSongsLoaded = true)
                }
        }
    }

    // 「All」 Tab
    fun loadMoreAllSongs() {
        val state = _uiState.value as? ArtistUiState.Success ?: return
        if (!state.allSongsHasMore || state.allSongsLoadingMore) return
        _uiState.value = state.copy(allSongsLoadingMore = true)
        viewModelScope.launch(Dispatchers.IO) {
            artistRepository.getArtistAllSongs(currentArtistId, offset = allSongsOffset, limit = ALL_SONGS_PAGE_SIZE)
                .first()
                .onSuccess { page ->
                    allSongsOffset += page.songs.size
                    val latest = _uiState.value as? ArtistUiState.Success ?: return@onSuccess
                    _uiState.value = latest.copy(
                        allSongs = latest.allSongs + page.songs,
                        allSongsHasMore = page.hasMore,
                        allSongsLoadingMore = false
                    )
                }
                .onFailure { e ->
                    AppLogger.w(TAG, "Failed to load more songs: artistId=$currentArtistId", e)
                    val latest = _uiState.value as? ArtistUiState.Success ?: return@onFailure
                    _uiState.value = latest.copy(allSongsLoadingMore = false)
                }
        }
    }

    fun toggleFollow(artistId: Long) {
        val currentState = _uiState.value as? ArtistUiState.Success ?: return
        val targetSubscribe = !currentState.isFollowed
        viewModelScope.launch {
            artistRepository.subscribeArtist(artistId, targetSubscribe).collect { result ->
                result.onSuccess {
                    _uiState.value = currentState.copy(isFollowed = targetSubscribe)
                    val msg = if (targetSubscribe) "Followed artist" else "Unfollowed artist"
                    _toastEvent.emit(msg)
                    if (targetSubscribe) {
                        runCatching {
                            org.koin.core.context.GlobalContext.get().get<com.lin0721.linmusic.core.friend.FriendManager>()
                                .broadcastActivity("artist", currentState.artist.name, "Artist")
                        }
                    }
                }.onFailure { e ->
                    _toastEvent.emit(e.toUserMessage(resourceProvider))
                }
            }
        }
    }

    fun playSongInList(track: Track, allTracks: List<Track>) {
        val artistName = (_uiState.value as? ArtistUiState.Success)?.artist?.name ?: "Artist"
        val queueItems = allTracks.map { t ->
            QueueItem(t.id, t.name, t.ar.joinToString { it.name }, t.al.picUrl)
        }
        val targetIndex = queueItems.indexOfFirst { it.songId == track.id }.coerceAtLeast(0)
        playerManager.playQueue(queueItems, targetIndex, artistName)
    }

    // NextPlay
    fun addTrackToPlayNext(track: Track) {
        val queueItem = QueueItem(track.id, track.name, track.ar.joinToString("/") { it.name }, track.al.picUrl)
        playerManager.addToPlayNext(listOf(queueItem))
        viewModelScope.launch { _toastEvent.emit("Added to play next") }
    }

    fun prepareCollectDialog(songId: Long) {
        viewModelScope.launch {
            songCollectDelegate.prepare(songId, songLikeRepository.likedSongIds.value) { _toastEvent.emit(it) }
        }
    }

    fun savePlaylistCollection(songId: Long, items: List<PlaylistCollectItem>) {
        viewModelScope.launch {
            songCollectDelegate.save(
                songId = songId,
                items = items,
                likedSongIds = songLikeRepository.likedSongIds.value,
                onToast = { _toastEvent.emit(it) },
                onLikedChanged = { newLiked ->
                    songLikeRepository.syncLikedSongIds(newLiked)
                }
            )
        }
    }

    fun createPlaylistAndAddSong(name: String, songId: Long) {
        viewModelScope.launch {
            songCollectDelegate.createAndAdd(name, songId, songLikeRepository.likedSongIds.value) { _toastEvent.emit(it) }
        }
    }

    // Songs"Like"，「More Options」（ and  of Save）
    fun toggleLikeSong(songId: Long, like: Boolean) {
        viewModelScope.launch {
            songLikeRepository.likeSong(songId, like).collect { result ->
                result.onSuccess {
                    _toastEvent.emit(if (like) "Added to Liked Songs" else "Removed from Liked Songs")
                }.onFailure { e ->
                    _toastEvent.emit(e.toUserMessage(resourceProvider))
                }
            }
        }
    }

    fun handleLoginSuccess(cookies: String) {
        viewModelScope.launch {
            if (syncProfileAfterLoginUseCase(cookies) == null) return@launch
            _toastEvent.emit("Logged in successfully, syncing data...")
            // Artists，Follow and
            loadLikedSongIds()
            (uiState.value as? ArtistUiState.Success)?.let { successState ->
                loadArtistData(successState.artist.id)
            }
        }
    }
}
