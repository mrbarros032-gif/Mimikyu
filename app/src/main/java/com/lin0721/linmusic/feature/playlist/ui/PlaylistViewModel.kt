package com.lin0721.linmusic.feature.playlist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.core.auth.UserPreferences
import com.lin0721.linmusic.core.auth.UserProfile
import com.lin0721.linmusic.core.download.DownloadTrackInfo
import com.lin0721.linmusic.core.download.SongDownloadManager
import com.lin0721.linmusic.core.download.yearFromEpochMillis
import com.lin0721.linmusic.core.model.PlaylistDetail
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.auth.SyncProfileAfterLoginUseCase
import com.lin0721.linmusic.core.songlike.LoadLikedSongIdsUseCase
import com.lin0721.linmusic.feature.playlist.domain.CreatePlaylistAndAddSongUseCase
import com.lin0721.linmusic.feature.playlist.domain.SongCollectDelegate
import com.lin0721.linmusic.feature.playlist.domain.UpdatePlaylistCoverUseCase
import com.lin0721.linmusic.feature.home.data.HomeRepository
import com.lin0721.linmusic.feature.library.data.LibraryRepository
import com.lin0721.linmusic.core.songlike.SongLikeRepository
import com.lin0721.linmusic.feature.playlist.data.PlaylistRepository
import com.lin0721.linmusic.core.player.data.PlaybackRepository
import com.lin0721.linmusic.core.player.PlayerManager
import com.lin0721.linmusic.core.player.QueueItem
import com.lin0721.linmusic.core.userplaylist.UserPlaylistRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.lin0721.linmusic.feature.home.data.DailySong
import com.lin0721.linmusic.core.playlistmutation.PlaylistMutationBus
import com.lin0721.linmusic.core.playlistmutation.PlaylistMutationEvent
import com.lin0721.linmusic.core.ui.components.PlaylistCollectItem
import com.lin0721.linmusic.core.ui.components.PlaylistCollectState
import com.lin0721.linmusic.core.network.ResourceProvider
import com.lin0721.linmusic.core.network.toUserMessage

import com.lin0721.linmusic.feature.search.data.SearchRepository
import com.lin0721.linmusic.feature.search.domain.SearchResultItem
import com.lin0721.linmusic.feature.search.domain.SearchType
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.PlaylistItem
import com.lin0721.linmusic.core.contentfilter.QualityFilter
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import com.lin0721.linmusic.core.metadata.YouTubeThumbnailUpscaler
import com.lin0721.linmusic.core.metadata.cleanMetadataText
import com.lin0721.linmusic.core.lastfm.LastFmRepository
import com.lin0721.linmusic.core.log.AppLogger

// Playlists， song/detail
private const val TRACK_PAGE_SIZE = 1000

class PlaylistViewModel(
    private val songCollectDelegate: SongCollectDelegate,
    private val syncProfileAfterLoginUseCase: SyncProfileAfterLoginUseCase,
    private val loadLikedSongIdsUseCase: LoadLikedSongIdsUseCase,
    private val homeRepository: HomeRepository,
    private val libraryRepository: LibraryRepository,
    private val playlistRepository: PlaylistRepository,
    private val songLikeRepository: SongLikeRepository,
    private val playbackRepository: PlaybackRepository,
    private val userPlaylistRepository: UserPlaylistRepository,
    private val createPlaylistAndAddSongUseCase: CreatePlaylistAndAddSongUseCase,
    private val updatePlaylistCoverUseCase: UpdatePlaylistCoverUseCase,
    val playerManager: PlayerManager,
    private val userPreferences: UserPreferences,
    private val resourceProvider: ResourceProvider,
    private val playlistMutationBus: PlaylistMutationBus,
    private val searchRepository: SearchRepository,
    private val songDownloadManager: SongDownloadManager,
    private val dailyMixGenerator: com.lin0721.linmusic.core.player.recommendation.DailyMixGenerator? = null,
    private val lastFmRepository: LastFmRepository? = null
) : ViewModel() {

    private var allRecommendedTracks = listOf<Track>()
    private var currentRecIndex = 0

    private var isAlbumMode = false
    private var loadJob: Job? = null
    private var youMightLikeJob: Job? = null

    private val _uiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Loading)
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    val userProfile = userPreferences.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val likedSongIds: StateFlow<Set<Long>> = songLikeRepository.likedSongIds

    val collectState: StateFlow<PlaylistCollectState> = songCollectDelegate.state



    // Daily History（Daily Recommendations/Listening Rank）
    private val _historyRecommendState = MutableStateFlow(HistoryRecommendState())
    val historyRecommendState: StateFlow<HistoryRecommendState> = _historyRecommendState.asStateFlow()

    // "Add to playlist"
    private val _importState = MutableStateFlow(PlaylistImportState())
    val importState: StateFlow<PlaylistImportState> = _importState.asStateFlow()

    // Edit Playlist Info（//）Save of ，Save
    private val _isSavingInfo = MutableStateFlow(false)
    val isSavingInfo: StateFlow<Boolean> = _isSavingInfo.asStateFlow()

    init {
        loadLikedSongIds()
    }

    fun loadLikedSongIds() {
        viewModelScope.launch {
            loadLikedSongIdsUseCase()
        }
    }

    fun loadPlaylist(id: Long, isAlbum: Boolean = false) {
        isAlbumMode = isAlbum
        _uiState.value = PlaylistUiState.Loading
        allRecommendedTracks = emptyList()
        loadJob?.cancel() // Cancel of
        youMightLikeJob?.cancel()
        if (id <= -10L || id == -3L || (id in -9L..-1L && com.lin0721.linmusic.core.player.recommendation.DailyMixGenerator.getCachedMix(id) != null)) {
            loadDailyMix(id)
            return
        }
        if (id == -2L) {
            _historyRecommendState.update { it.copy(selectedDate = "Past Week") }
            loadJob = viewModelScope.launch {
                loadUserRecord(1)
            }
            return
        }
        if (id == -1L) {
            _historyRecommendState.update { it.copy(selectedDate = "Today") }
            loadJob = viewModelScope.launch {
                homeRepository.getDailyRecommendSongs().collect { result ->
                    result.fold(
                        onSuccess = { dailySongs ->
                            val tracks = dailySongs.map { song ->
                                Track(
                                    id = song.id,
                                    name = song.name,
                                    ar = song.ar,
                                    al = song.al,
                                    fee = song.fee
                                )
                            }
                            val detail = PlaylistDetail(
                                id = -1L,
                                name = "Daily Mix",
                                coverImgUrl = tracks.firstOrNull()?.al?.picUrl ?: "",
                                description = "Personalized daily songs for you",
                                playCount = 0L,
                                tracks = tracks
                            )
                            _uiState.value = PlaylistUiState.Success(detail)
                            preResolveFirst3Tracks(detail.tracks)
                            allRecommendedTracks = emptyList()
                            loadYouMightLike(detail)
                        },
                        onFailure = { error ->
                            _uiState.value = PlaylistUiState.Error(error.toUserMessage(resourceProvider))
                        }
                    )
                }
            }
            return
        }
        loadJob = viewModelScope.launch {
            val flow = if (isAlbum) playlistRepository.getAlbumDetail(id) else playlistRepository.getPlaylistDetail(id)
            flow.collect { result ->
                result.fold(
                    onSuccess = { detail ->
                        _uiState.value = PlaylistUiState.Success(
                            detail,
                            isSubscribed = detail.subscribed,
                            hasMoreTracks = detail.trackIds.size > detail.tracks.size
                        )
                        preResolveFirst3Tracks(detail.tracks)
                        val baseSong = detail.tracks.firstOrNull()

                        if (baseSong != null && !isAlbum) {
                            loadRecommendations(detail.tracks)
                        } else {
                            allRecommendedTracks = emptyList()
                        }
                        // AlbumsDetailsSave，SavedAlbums
                        if (isAlbum) {
                            checkAlbumSubscribed(detail.id)
                        }
                        loadYouMightLike(detail)
                    },
                    onFailure = { error ->
                        if (id in listOf(3778678L, 19723756L, -20L)) {
                            loadChartFallback(id)
                        } else {
                            _uiState.value = PlaylistUiState.Error(error.toUserMessage(resourceProvider))
                        }
                    }
                )
            }
        }
    }

    private fun loadDailyMix(id: Long) {
        val cached = com.lin0721.linmusic.core.player.recommendation.DailyMixGenerator.getCachedMix(id)
        if (cached != null && cached.tracks.isNotEmpty()) {
            val tracks = cached.tracks.map { qi ->
                val studioCover = YouTubeIdRegistry.getCoverUrl(qi.songId).takeIf { it.isNotBlank() } ?: qi.coverUrl
                Track(
                    id = qi.songId,
                    name = qi.title,
                    ar = qi.artist.split(",", "/").map {
                        com.lin0721.linmusic.core.model.Artist(
                            id = it.trim().hashCode().toLong() and 0x7FFFFFFFL,
                            name = it.trim()
                        )
                    },
                    al = com.lin0721.linmusic.core.model.Album(
                        id = (qi.album ?: "").hashCode().toLong() and 0x7FFFFFFFL,
                        name = qi.album ?: "",
                        picUrl = studioCover
                    ),
                    dt = qi.durationSec.toLong() * 1000L
                )
            }
            val detail = PlaylistDetail(
                id = cached.id,
                name = cached.title,
                coverImgUrl = cached.coverUrl,
                description = "Made for you • " + cached.subtitle,
                playCount = 0L,
                tracks = tracks
            )
            _uiState.value = PlaylistUiState.Success(detail)
            preResolveFirst3Tracks(tracks)
            allRecommendedTracks = emptyList()
            loadYouMightLike(detail)
            return
        }

        loadJob = viewModelScope.launch {
            try {
                val generator = dailyMixGenerator ?: com.lin0721.linmusic.core.player.recommendation.DailyMixGenerator()
                val fullMix = generator.getOrGeneratePlaylist(id)
                val targetMix = fullMix ?: com.lin0721.linmusic.core.player.recommendation.DailyMixGenerator.getCachedMix(id)

                if (targetMix != null && targetMix.tracks.isNotEmpty()) {
                    val tracks = targetMix.tracks.map { qi ->
                        val studioCover = YouTubeIdRegistry.getCoverUrl(qi.songId).takeIf { it.isNotBlank() } ?: qi.coverUrl
                        Track(
                            id = qi.songId,
                            name = qi.title,
                            ar = qi.artist.split(",", "/").map {
                                com.lin0721.linmusic.core.model.Artist(
                                    id = it.trim().hashCode().toLong() and 0x7FFFFFFFL,
                                    name = it.trim()
                                )
                            },
                            al = com.lin0721.linmusic.core.model.Album(
                                id = (qi.album ?: "").hashCode().toLong() and 0x7FFFFFFFL,
                                name = qi.album ?: "",
                                picUrl = studioCover
                            ),
                            dt = qi.durationSec.toLong() * 1000L
                        )
                    }
                    val detail = PlaylistDetail(
                        id = targetMix.id,
                        name = targetMix.title,
                        coverImgUrl = targetMix.coverUrl,
                        description = "Made for you • " + targetMix.subtitle,
                        playCount = 0L,
                        tracks = tracks
                    )
                    _uiState.value = PlaylistUiState.Success(detail)
                    preResolveFirst3Tracks(tracks)
                    loadYouMightLike(detail)
                } else {
                    _uiState.value = PlaylistUiState.Error("Unable to generate Daily Mix at this time")
                }
            } catch (e: Exception) {
                _uiState.value = PlaylistUiState.Error(e.toUserMessage(resourceProvider))
            }
        }
    }

    private fun loadChartFallback(id: Long) {
        val chartTitle = if (id == 3778678L) "Top 50 - Global" else "Top 100 Hits 2026"
        val query = if (id == 3778678L) "Top 50 Global" else "Top 100 Hits"
        loadJob = viewModelScope.launch {
            searchRepository.search(query, SearchType.SONG, limit = 50).collect { result ->
                result.fold(
                    onSuccess = { page ->
                        val tracks = page.items.filterIsInstance<SearchResultItem.SongItem>().map { it.track }
                        if (tracks.isNotEmpty()) {
                            _uiState.value = PlaylistUiState.Success(
                                PlaylistDetail(
                                    id = id,
                                    name = chartTitle,
                                    coverImgUrl = tracks.firstOrNull()?.al?.picUrl ?: "",
                                    description = "Your daily update of the most played tracks.",
                                    playCount = 0L,
                                    tracks = tracks
                                )
                            )
                            preResolveFirst3Tracks(tracks)
                        } else {
                            _uiState.value = PlaylistUiState.Error("No chart songs available")
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = PlaylistUiState.Error(error.toUserMessage(resourceProvider))
                    }
                )
            }
        }
    }

    private fun preResolveFirst3Tracks(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val queueItems = tracks.take(3).map { t ->
            QueueItem(
                songId = t.id,
                title = t.name,
                artist = t.ar.joinToString { it.name },
                coverUrl = t.al.picUrl,
                album = t.al.name,
                durationSec = (t.dt / 1000).toInt()
            )
        }
        playerManager.preResolveTracks(queueItems)
    }

    private fun checkAlbumSubscribed(albumId: Long) {
        viewModelScope.launch {
            libraryRepository.getCollectedAlbums().collect { result ->
                result.onSuccess { albums ->
                    val subscribed = albums.any { it.id == albumId }
                    _uiState.update { state ->
                        if (state is PlaylistUiState.Success && state.playlist.id == albumId) {
                            state.copy(isSubscribed = subscribed)
                        } else state
                    }
                }
            }
        }
    }

    // Playlists，
    fun loadMoreTracks() {
        val current = _uiState.value as? PlaylistUiState.Success ?: return
        if (!current.hasMoreTracks || current.isLoadingMoreTracks) return
        val allIds = current.playlist.trackIds.map { it.id }
        val nextIds = allIds.drop(current.playlist.tracks.size).take(TRACK_PAGE_SIZE)
        if (nextIds.isEmpty()) {
            _uiState.update { state -> if (state is PlaylistUiState.Success) state.copy(hasMoreTracks = false) else state }
            return
        }
        val playlistId = current.playlist.id
        _uiState.update { state -> if (state is PlaylistUiState.Success) state.copy(isLoadingMoreTracks = true) else state }
        viewModelScope.launch {
            playlistRepository.loadMoreTracks(nextIds).collect { result ->
                result.fold(
                    onSuccess = { newTracks ->
                        _uiState.update { state ->
                            if (state is PlaylistUiState.Success && state.playlist.id == playlistId) {
                                val updatedTracks = state.playlist.tracks + newTracks
                                state.copy(
                                    playlist = state.playlist.copy(tracks = updatedTracks),
                                    hasMoreTracks = updatedTracks.size < allIds.size,
                                    isLoadingMoreTracks = false
                                )
                            } else state
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { state -> if (state is PlaylistUiState.Success) state.copy(isLoadingMoreTracks = false) else state }
                        _toastEvent.emit(e.toUserMessage(resourceProvider))
                    }
                )
            }
        }
    }

    // stopWhen All； uiState Back，Back null  toast
    private suspend fun loadTracksUntil(stopWhen: (List<Track>) -> Boolean): List<Track>? {
        val current = _uiState.value as? PlaylistUiState.Success ?: return null
        val playlistId = current.playlist.id
        val allIds = current.playlist.trackIds.map { it.id }
        val loadedTracks = mutableListOf<Track>().apply { addAll(current.playlist.tracks) }
        var failure: Throwable? = null
        if (!stopWhen(loadedTracks)) {
            for (chunk in allIds.drop(loadedTracks.size).chunked(TRACK_PAGE_SIZE)) {
                val result = playlistRepository.loadMoreTracks(chunk).first()
                result.fold(
                    onSuccess = { loadedTracks.addAll(it) },
                    onFailure = { e -> failure = e }
                )
                if (failure != null || stopWhen(loadedTracks)) break
            }
        }
        if (failure != null) {
            _toastEvent.emit(failure.toUserMessage(resourceProvider))
            return null
        }
        _uiState.update { state ->
            if (state is PlaylistUiState.Success && state.playlist.id == playlistId) {
                state.copy(
                    playlist = state.playlist.copy(tracks = loadedTracks),
                    hasMoreTracks = loadedTracks.size < allIds.size,
                    isLoadingMoreTracks = false
                )
            } else state
        }
        return loadedTracks
    }

    // PlaylistsAll：（）、AllSongs，
    // of ，1000 of Playlists of
    fun ensureAllTracksLoaded(onReady: (List<Track>) -> Unit) {
        val current = _uiState.value as? PlaylistUiState.Success ?: return
        if (!current.hasMoreTracks) {
            onReady(current.playlist.tracks)
            return
        }
        if (current.isLoadingMoreTracks) return
        _uiState.update { state -> if (state is PlaylistUiState.Success) state.copy(isLoadingMoreTracks = true) else state }
        viewModelScope.launch {
            val result = loadTracksUntil { false }
            if (result != null) {
                onReady(result)
            } else {
                _uiState.update { state -> if (state is PlaylistUiState.Success) state.copy(isLoadingMoreTracks = false) else state }
            }
        }
    }

    // ：CurrentPlaySongs， of  ensureAllTracksLoaded
    fun ensureTrackLoaded(trackId: Long, onReady: (List<Track>) -> Unit = {}) {
        val current = _uiState.value as? PlaylistUiState.Success ?: return
        if (current.playlist.tracks.any { it.id == trackId }) {
            onReady(current.playlist.tracks)
            return
        }
        if (current.isLoadingMoreTracks) return
        _uiState.update { state -> if (state is PlaylistUiState.Success) state.copy(isLoadingMoreTracks = true) else state }
        viewModelScope.launch {
            val result = loadTracksUntil { tracks -> tracks.any { it.id == trackId } }
            if (result != null) {
                onReady(result)
            } else {
                _uiState.update { state -> if (state is PlaylistUiState.Success) state.copy(isLoadingMoreTracks = false) else state }
            }
        }
    }

    fun playSongInList(track: Track, allTracks: List<Track>) {
        val playlistName = (_uiState.value as? PlaylistUiState.Success)?.playlist?.name ?: "Playlist"
        val queueItems = allTracks.map { t ->
            val vid = YouTubeIdRegistry.getVideoId(t.id).orEmpty()
            val rawCover = YouTubeIdRegistry.getCoverUrl(t.id).takeIf { it.isNotBlank() } ?: t.al.picUrl
            val studioCover = com.lin0721.linmusic.core.metadata.YouTubeThumbnailUpscaler.upscale(rawCover) ?: rawCover
            QueueItem(
                songId = t.id,
                title = t.name,
                artist = t.ar.joinToString { it.name },
                coverUrl = studioCover,
                videoId = vid.ifBlank { null },
                album = t.al.name.takeIf { it.isNotBlank() },
                durationSec = (t.dt / 1000).toInt(),
                localUri = t.localUri
            )
        }
        val targetIndex = queueItems.indexOfFirst { it.songId == track.id }.coerceAtLeast(0)
        playerManager.playQueue(queueItems, targetIndex, playlistName)
    }

    fun playAllLinear(tracks: List<Track>) {
        val playlistName = (_uiState.value as? PlaylistUiState.Success)?.playlist?.name ?: "Playlist"
        val queueItems = tracks.map { t ->
            val vid = YouTubeIdRegistry.getVideoId(t.id).orEmpty()
            val rawCover = YouTubeIdRegistry.getCoverUrl(t.id).takeIf { it.isNotBlank() } ?: t.al.picUrl
            val studioCover = com.lin0721.linmusic.core.metadata.YouTubeThumbnailUpscaler.upscale(rawCover) ?: rawCover
            QueueItem(
                songId = t.id,
                title = t.name,
                artist = t.ar.joinToString { it.name },
                coverUrl = studioCover,
                videoId = vid.ifBlank { null },
                album = t.al.name.takeIf { it.isNotBlank() },
                durationSec = (t.dt / 1000).toInt(),
                localUri = t.localUri
            )
        }
        playerManager.playQueue(queueItems, 0, playlistName)
    }

    fun addTrackToPlayNext(track: Track) {
        val vid = YouTubeIdRegistry.getVideoId(track.id).orEmpty()
        val rawCover = YouTubeIdRegistry.getCoverUrl(track.id).takeIf { it.isNotBlank() } ?: track.al.picUrl
        val studioCover = com.lin0721.linmusic.core.metadata.YouTubeThumbnailUpscaler.upscale(rawCover) ?: rawCover
        val queueItem = QueueItem(
            songId = track.id,
            title = track.name,
            artist = track.ar.joinToString("/") { it.name },
            coverUrl = studioCover,
            videoId = vid.ifBlank { null },
            album = track.al.name.takeIf { it.isNotBlank() },
            durationSec = (track.dt / 1000).toInt(),
            localUri = track.localUri
        )
        playerManager.addToPlayNext(listOf(queueItem))
        viewModelScope.launch { _toastEvent.emit("Added to play next") }
    }

    fun addTracksToPlayNext(tracks: List<Track>) {
        val queueItems = tracks.map { t ->
            val vid = YouTubeIdRegistry.getVideoId(t.id).orEmpty()
            val studioCover = YouTubeIdRegistry.getCoverUrl(t.id).takeIf { it.isNotBlank() } ?: t.al.picUrl
            QueueItem(
                songId = t.id,
                title = t.name,
                artist = t.ar.joinToString("/") { it.name },
                coverUrl = studioCover,
                videoId = vid.ifBlank { null },
                album = t.al.name.takeIf { it.isNotBlank() },
                durationSec = (t.dt / 1000).toInt(),
                localUri = t.localUri
            )
        }
        playerManager.addToPlayNext(queueItems)
        viewModelScope.launch { _toastEvent.emit("Added ${queueItems.size} songs to play queue") }
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
            // “Liked Songs”Playlists，Cancel of SongsCurrent
            val successState = _uiState.value as? PlaylistUiState.Success
            val profile = userPreferences.userProfile.first()
            if (successState != null && profile != null && successState.playlist.id == profile.uid) {
                loadPlaylist(successState.playlist.id, isAlbumMode)
            }
        }
    }

    fun createPlaylistAndAddSong(name: String, songId: Long) {
        viewModelScope.launch {
            songCollectDelegate.createAndAdd(name, songId, songLikeRepository.likedSongIds.value) { _toastEvent.emit(it) }
        }
    }

    fun handleLoginSuccess(cookies: String) {
        viewModelScope.launch {
            _toastEvent.emit("Logged in successfully, syncing data...")
            if (syncProfileAfterLoginUseCase(cookies) != null) {
                loadLikedSongIds()
            }
        }
    }

    // 「Playlists、RecommendedSongs」 of ：playlist/detail/rcmd/get Back of PlaylistsSongs，
    // Heartbeat Mode「Liked Songs」Playlists，Playlists 400「Playlists」。
    // PlaylistsSongs，Playlists of
    fun loadRecommendations(tracks: List<Track>) {
        val seedIds = pickRecommendSeedIds(tracks)
        if (seedIds.isEmpty()) {
            allRecommendedTracks = emptyList()
            setRecommendedSongs(emptyList())
            return
        }
        viewModelScope.launch {
            val results = seedIds.map { seedId ->
                async {
                    runCatching { playbackRepository.getSimilarSongs(seedId).first() }
                        .getOrElse { Result.failure(it) }
                }
            }.awaitAll()

            // ，All
            val succeeded = results.filter { it.isSuccess }
            if (succeeded.isEmpty()) {
                allRecommendedTracks = emptyList()
                setRecommendedSongs(emptyList())
                return@launch
            }

            val existingIds = tracks.mapTo(mutableSetOf()) { it.id }
            allRecommendedTracks = succeeded
                .flatMap { it.getOrDefault(emptyList()) }
                .distinctBy { it.id }
                .filterNot { it.id in existingIds }
            currentRecIndex = 0
            updateCurrentRecommendations()
        }
    }

    // 、、，Playlists；Playlists
    private fun pickRecommendSeedIds(tracks: List<Track>): List<Long> {
        if (tracks.isEmpty()) return emptyList()
        return listOf(0, tracks.size / 2, tracks.lastIndex)
            .distinct()
            .map { tracks[it].id }
            .distinct()
    }

    private fun setRecommendedSongs(songs: List<Track>) {
        _uiState.update { state ->
            if (state is PlaylistUiState.Success) state.copy(recommendedSongs = songs) else state
        }
    }

    private fun updateCurrentRecommendations() {
        if (allRecommendedTracks.isEmpty()) {
            setRecommendedSongs(emptyList())
            return
        }
        val size = allRecommendedTracks.size
        val start = currentRecIndex % size
        val list = mutableListOf<Track>()
        for (i in 0 until 5) {
            val idx = (start + i) % size
            val track = allRecommendedTracks[idx]
            if (!list.contains(track)) {
                list.add(track)
            }
            if (list.size >= size) break
        }
        setRecommendedSongs(list)
    }

    fun refreshRecommendations() {
        if (allRecommendedTracks.isNotEmpty()) {
            currentRecIndex = (currentRecIndex + 5) % allRecommendedTracks.size
            updateCurrentRecommendations()
        }
    }

    private fun loadYouMightLike(detail: PlaylistDetail) {
        youMightLikeJob?.cancel()
        youMightLikeJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Gather artist signals from playlist tracks
                val artistFrequency = mutableMapOf<String, Int>()
                detail.tracks.forEach { track ->
                    track.ar.forEach { artist ->
                        val name = artist.name.trim()
                        if (name.isNotBlank() && !name.equals("Various Artists", ignoreCase = true) && !name.equals("Unknown", ignoreCase = true)) {
                            artistFrequency[name] = (artistFrequency[name] ?: 0) + 1
                        }
                    }
                }

                val topArtists = artistFrequency.entries
                    .sortedByDescending { it.value }
                    .map { it.key }
                    .take(5)

                val primaryArtist = topArtists.firstOrNull()

                // 2. Discover culturally similar artists & genre tags using Last.fm
                val similarArtists = if (!primaryArtist.isNullOrBlank() && lastFmRepository != null) {
                    runCatching {
                        lastFmRepository.getSimilarArtists(primaryArtist, limit = 6)
                    }.getOrDefault(emptyList())
                } else emptyList()

                val lastFmInfo = if (!primaryArtist.isNullOrBlank() && lastFmRepository != null) {
                    runCatching {
                        lastFmRepository.getArtistInfo(primaryArtist)
                    }.getOrNull()
                } else null

                val genreTag = lastFmInfo?.topTag?.takeIf { it.isNotBlank() }
                    ?: lastFmInfo?.tags?.firstOrNull()?.takeIf { it.isNotBlank() }

                // 3. Formulate targeted search queries for InnerTube playlists and albums
                val queries = mutableListOf<Pair<String, YouTube.SearchFilter>>()

                if (!primaryArtist.isNullOrBlank()) {
                    queries.add("$primaryArtist Mix" to YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                    queries.add("$primaryArtist" to YouTube.SearchFilter.FILTER_ALBUM)
                }

                similarArtists.take(2).forEach { similar ->
                    queries.add("$similar Mix" to YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                    queries.add("$similar playlist" to YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST)
                }

                topArtists.drop(1).take(2).forEach { artist ->
                    queries.add("$artist Mix" to YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                }

                if (!genreTag.isNullOrBlank()) {
                    queries.add("$genreTag Mix" to YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                    queries.add("$genreTag" to YouTube.SearchFilter.FILTER_ALBUM)
                }

                val cleanPlaylistName = cleanMetadataText(detail.name)
                val isGenericName = cleanPlaylistName.equals("Liked Songs", ignoreCase = true) ||
                        cleanPlaylistName.equals("Daily Mix", ignoreCase = true) ||
                        cleanPlaylistName.equals("Favorites", ignoreCase = true) ||
                        cleanPlaylistName.equals("History", ignoreCase = true)
                if (!isGenericName && cleanPlaylistName.isNotBlank()) {
                    queries.add("$cleanPlaylistName playlist" to YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST)
                }

                if (queries.size < 3) {
                    queries.add("Top Hits Mix" to YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                    queries.add("Today Top Hits" to YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST)
                }

                // 4. Concurrently query YouTube Music InnerTube
                val deferredResults = queries.take(6).map { (query, filter) ->
                    async {
                        runCatching {
                            YouTube.search(query, filter).getOrNull()
                        }.getOrNull()
                    }
                }

                val searchResponses = deferredResults.awaitAll().filterNotNull()

                // 5. Filter, map & anti-spam verify into CuratedMixItem
                val seenTitles = mutableSetOf<String>()
                seenTitles.add(cleanPlaylistName.lowercase())

                val curatedList = mutableListOf<CuratedMixItem>()

                for (response in searchResponses) {
                    for (item in response.items) {
                        when (item) {
                            is PlaylistItem -> {
                                val title = cleanMetadataText(item.title)
                                val rawAuthor = item.author?.name?.takeIf { !it.equals("YouTube Music", ignoreCase = true) }
                                val author = cleanMetadataText(rawAuthor)
                                val normalizedTitle = title.lowercase().trim()

                                if (normalizedTitle !in seenTitles &&
                                    QualityFilter.isHighQualityRelease(title = title, artistName = author)
                                ) {
                                    seenTitles.add(normalizedTitle)
                                    val plId = YouTubeIdRegistry.registerPlaylist(item.id)
                                    val cover = YouTubeThumbnailUpscaler.upscale(item.thumbnail)
                                        ?: item.thumbnail.orEmpty()
                                    curatedList.add(
                                        CuratedMixItem(
                                            id = plId,
                                            title = title,
                                            subtitle = author.ifBlank { "Playlist" },
                                            coverUrl = cover,
                                            isAlbum = false
                                        )
                                    )
                                }
                            }
                            is AlbumItem -> {
                                val title = cleanMetadataText(item.title)
                                val artistName = cleanMetadataText(item.artists?.joinToString(", ") { cleanMetadataText(it.name) } ?: "")
                                val normalizedTitle = title.lowercase().trim()

                                if (normalizedTitle !in seenTitles &&
                                    QualityFilter.isHighQualityRelease(title = title, artistName = artistName)
                                ) {
                                    seenTitles.add(normalizedTitle)
                                    val albId = YouTubeIdRegistry.registerAlbum(item.browseId)
                                    val cover = YouTubeThumbnailUpscaler.upscale(item.thumbnail)
                                        ?: item.thumbnail.orEmpty()
                                    curatedList.add(
                                        CuratedMixItem(
                                            id = albId,
                                            title = title,
                                            subtitle = artistName.ifBlank { "Album" },
                                            coverUrl = cover,
                                            isAlbum = true
                                        )
                                    )
                                }
                            }
                            else -> Unit
                        }
                        if (curatedList.size >= 10) break
                    }
                    if (curatedList.size >= 10) break
                }

                if (curatedList.isNotEmpty()) {
                    _uiState.update { state ->
                        if (state is PlaylistUiState.Success && state.playlist.id == detail.id) {
                            state.copy(youMightLikeItems = curatedList)
                        } else state
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("PlaylistViewModel", "loadYouMightLike failed", e)
            }
        }
    }

    fun addRecommendSongToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            playlistRepository.manipulatePlaylistTracks("add", playlistId, listOf(track.id)).collect { result ->
                result.onSuccess {
                    _toastEvent.emit("Added to playlist")
                    allRecommendedTracks = allRecommendedTracks.filter { it.id != track.id }
                    updateCurrentRecommendations()

                    _uiState.update { state ->
                        if (state is PlaylistUiState.Success) {
                            val updatedTracks = state.playlist.tracks.toMutableList().apply {
                                if (none { it.id == track.id }) {
                                    add(track)
                                }
                            }
                            state.copy(playlist = state.playlist.copy(tracks = updatedTracks))
                        } else state
                    }
                }.onFailure { e ->
                    _toastEvent.emit(e.toUserMessage(resourceProvider))
                }
            }
        }
    }

    // CurrentPlaylistsDelete，PlaylistsCreate（ UI ）
    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            playlistRepository.manipulatePlaylistTracks("del", playlistId, listOf(trackId)).collect { result ->
                result.onSuccess {
                    _toastEvent.emit("Removed from playlist")
                    _uiState.update { state ->
                        if (state is PlaylistUiState.Success && state.playlist.id == playlistId) {
                            val updatedTracks = state.playlist.tracks.filter { it.id != trackId }
                            state.copy(playlist = state.playlist.copy(tracks = updatedTracks))
                        } else state
                    }
                }.onFailure { e ->
                    _toastEvent.emit(e.toUserMessage(resourceProvider))
                }
            }
        }
    }

    // Current of Playlists，"Add to playlist"；CurrentPlaylists and "Liked Songs"
    fun prepareImportTargets(excludePlaylistId: Long) {
        viewModelScope.launch {
            val profile = userPreferences.userProfile.first()
            if (profile == null) {
                _toastEvent.emit("Please log in first")
                return@launch
            }
            _importState.update { it.copy(isLoading = true) }
            userPlaylistRepository.getUserPlaylists(profile.uid).collect { result ->
                result.onSuccess { playlists ->
                    val items = playlists.filter {
                        it.userId == profile.uid && it.id != excludePlaylistId && it.id != profile.uid
                    }
                    _importState.value = PlaylistImportState(items = items, isLoading = false)
                }.onFailure { e ->
                    _importState.update { it.copy(isLoading = false) }
                    _toastEvent.emit(e.toUserMessage(resourceProvider))
                }
            }
        }
    }

    // CurrentPlaylistsAllSongs of Playlists
    fun importAllTracksTo(targetPlaylistId: Long) {
        ensureAllTracksLoaded { tracks ->
            if (tracks.isEmpty()) return@ensureAllTracksLoaded
            viewModelScope.launch {
                playlistRepository.manipulatePlaylistTracks("add", targetPlaylistId, tracks.map { it.id }).collect { result ->
                    result.onSuccess {
                        _toastEvent.emit("Imported ${tracks.size} songs")
                    }.onFailure { e ->
                        _toastEvent.emit(e.toUserMessage(resourceProvider))
                    }
                }
            }
        }
    }

    // New PlaylistCurrentPlaylistsAllSongs
    fun createPlaylistAndImportAll(name: String) {
        ensureAllTracksLoaded { tracks ->
            viewModelScope.launch {
                createPlaylistAndAddSongUseCase(name, tracks.map { it.id }).collect { result ->
                    result.onSuccess {
                        _toastEvent.emit("Created playlist and imported ${tracks.size} songs")
                    }.onFailure { e ->
                        _toastEvent.emit(e.toUserMessage(resourceProvider))
                    }
                }
            }
        }
    }

    fun toggleSubscribePlaylist() {
        val successState = _uiState.value as? PlaylistUiState.Success ?: return
        val id = successState.playlist.id
        val targetSubscribe = !successState.isSubscribed
        val resourceLabel = if (isAlbumMode) "Album" else "Playlist"
        viewModelScope.launch {
            val flow = if (isAlbumMode) {
                playlistRepository.subscribeAlbum(id, targetSubscribe)
            } else {
                playlistRepository.subscribePlaylist(id, targetSubscribe)
            }
            flow.collect { result ->
                result.onSuccess {
                    _uiState.update { state ->
                        if (state is PlaylistUiState.Success) state.copy(isSubscribed = targetSubscribe) else state
                    }
                    _toastEvent.emit(if (targetSubscribe) "Saved $resourceLabel" else "Removed from saved $resourceLabel")
                    if (targetSubscribe) {
                        runCatching {
                            val mgr = org.koin.core.context.GlobalContext.get().get<com.lin0721.linmusic.core.friend.FriendManager>()
                            if (isAlbumMode) {
                                mgr.broadcastActivity("album", successState.playlist.name, "Album")
                            } else {
                                mgr.broadcastActivity("playlist", successState.playlist.name, "Playlist")
                            }
                        }
                    }
                }.onFailure { e ->
                    _toastEvent.emit(e.toUserMessage(resourceProvider))
                }
            }
        }
    }



    // Daily History
    fun loadHistoryDates() {
        viewModelScope.launch {
            _historyRecommendState.update { it.copy(datesLoading = true) }
            homeRepository.getHistoryRecommendDates().collect { result ->
                result.onSuccess { dates ->
                    _historyRecommendState.update { it.copy(dates = dates) }
                }.onFailure {
                    _toastEvent.emit(it.toUserMessage(resourceProvider))
                }
                _historyRecommendState.update { it.copy(datesLoading = false) }
            }
        }
    }

    // of Daily HistorySongs
    fun loadHistoryDetail(date: String) {
        if (date == "weekly" || date == "all") {
            val type = if (date == "weekly") 1 else 0
            loadUserRecord(type)
            return
        }
        viewModelScope.launch {
            _historyRecommendState.update { it.copy(selectedDate = date, songsLoading = true) }
            homeRepository.getHistoryRecommendDetail(date).collect { result ->
                result.onSuccess { songs ->
                    _historyRecommendState.update { it.copy(songs = songs) }
                    _uiState.update { state ->
                        if (state is PlaylistUiState.Success && state.playlist.id == -1L) {
                            val newTracks = songs.map { song ->
                                Track(
                                    id = song.id,
                                    name = song.name,
                                    ar = song.ar,
                                    al = song.al,
                                    fee = song.fee
                                )
                            }
                            state.copy(playlist = state.playlist.copy(tracks = newTracks))
                        } else state
                    }
                }.onFailure {
                    _toastEvent.emit(it.toUserMessage(resourceProvider))
                }
                _historyRecommendState.update { it.copy(songsLoading = false) }
            }
        }
    }

    fun loadUserRecord(type: Int) {
        viewModelScope.launch {
            val dateLabel = if (type == 1) "Past Week" else "All Time"
            _historyRecommendState.update { it.copy(selectedDate = dateLabel, songsLoading = true) }
            val profile = userPreferences.userProfile.first()
            if (profile == null) {
                _toastEvent.emit("Please log in")
                _historyRecommendState.update { it.copy(songsLoading = false) }
                return@launch
            }
            libraryRepository.getUserRecord(profile.uid, type).collect { result ->
                result.fold(
                    onSuccess = { records ->
                        val tracks = records.map { it.track }
                        val detail = PlaylistDetail(
                            id = -2L,
                            name = "Listening Rank Playlist",
                            coverImgUrl = tracks.firstOrNull()?.al?.picUrl ?: "",
                            description = "Statistics based on your listening history",
                            playCount = 0L,
                            tracks = tracks
                        )
                        _uiState.value = PlaylistUiState.Success(
                            playlist = detail,
                            trackPlayCounts = records.associate { it.track.id to it.playCount }
                        )
                        preResolveFirst3Tracks(tracks)
                        _historyRecommendState.update { it.copy(songsLoading = false) }
                    },
                    onFailure = { error ->
                        _toastEvent.emit(error.toUserMessage(resourceProvider))
                        _historyRecommendState.update { it.copy(songsLoading = false) }
                    }
                )
            }
        }
    }

    fun playHistorySong(index: Int) {
        val songs = _historyRecommendState.value.songs
        if (songs.isEmpty()) return
        val queueItems = songs.map { song ->
            QueueItem(song.id, song.name, song.ar.joinToString { it.name }, song.al.picUrl)
        }
        playerManager.playQueue(queueItems, index.coerceIn(0, queueItems.size - 1), "Daily History")
    }

    fun toggleLikeSong(songId: Long, isLike: Boolean) {
        viewModelScope.launch {
            songLikeRepository.likeSong(songId, isLike).collect { result ->
                result.fold(
                    onSuccess = {
                        _toastEvent.emit(if (isLike) "Added to Liked Songs" else "Removed from Liked Songs")

                        val successState = _uiState.value as? PlaylistUiState.Success
                        if (successState != null) {
                            val profile = userPreferences.userProfile.first()
                            if (profile != null && successState.playlist.id == profile.uid) {
                                loadPlaylist(successState.playlist.id, isAlbumMode)
                            }
                        }
                    },
                    onFailure = { e ->
                        _toastEvent.emit(e.toUserMessage(resourceProvider))
                    }
                )
            }
        }
    }

    // Playlists（ and ）
    fun updatePlaylistInfo(
        id: Long,
        originalName: String,
        newName: String,
        originalDesc: String?,
        newDesc: String,
        coverBytes: ByteArray?,
        onComplete: (Boolean) -> Unit
    ) {
        if (_isSavingInfo.value) return
        val nameChanged = newName.isNotBlank() && newName != originalName
        val descChanged = newDesc != (originalDesc ?: "")

        if (!nameChanged && !descChanged && coverBytes == null) {
            onComplete(true)
            return
        }

        viewModelScope.launch {
            _isSavingInfo.value = true
            try {

            if (coverBytes != null) {
                val newCoverUrl = updatePlaylistCoverUseCase(id, coverBytes).getOrElse { e ->
                    _toastEvent.emit(e.toUserMessage(resourceProvider))
                    onComplete(false)
                    return@launch
                }
                _uiState.update { state ->
                    if (state is PlaylistUiState.Success && state.playlist.id == id) {
                        state.copy(playlist = state.playlist.copy(coverImgUrl = newCoverUrl))
                    } else state
                }
                playlistMutationBus.emit(PlaylistMutationEvent.CoverUpdated(id, newCoverUrl))

                if (!nameChanged && !descChanged) {
                    _toastEvent.emit("Cover updated")
                    onComplete(true)
                    return@launch
                }
            }

            val nameDeferred = if (nameChanged) {
                async { playlistRepository.renamePlaylist(id, newName).first() }
            } else null

            val descDeferred = if (descChanged) {
                async { playlistRepository.updateDescription(id, newDesc).first() }
            } else null

            val nameResult = nameDeferred?.await()
            val descResult = descDeferred?.await()

            val nameSuccess = nameResult == null || nameResult.isSuccess
            val descSuccess = descResult == null || descResult.isSuccess

            val currentState = _uiState.value as? PlaylistUiState.Success
            if (currentState != null && (nameSuccess || descSuccess)) {
                val updatedPlaylist = currentState.playlist.copy(
                    name = if (nameSuccess && nameChanged) newName else currentState.playlist.name,
                    description = if (descSuccess && descChanged) newDesc else currentState.playlist.description
                )
                _uiState.value = currentState.copy(playlist = updatedPlaylist)
            }

            if (nameSuccess && nameChanged) {
                playlistMutationBus.emit(PlaylistMutationEvent.Renamed(id, newName))
            }
            if (descSuccess && descChanged) {
                playlistMutationBus.emit(PlaylistMutationEvent.DescriptionUpdated(id, newDesc))
            }

            if (nameSuccess && descSuccess) {
                _toastEvent.emit("Playlist info updated")
                onComplete(true)
            } else if (nameSuccess && !descSuccess) {
                val descMsg = descResult?.exceptionOrNull()?.toUserMessage(resourceProvider) ?: "Unknown error"
                _toastEvent.emit("Playlist name updated, failed to update description: $descMsg")
                onComplete(false)
            } else if (!nameSuccess && descSuccess) {
                val nameMsg = nameResult?.exceptionOrNull()?.toUserMessage(resourceProvider) ?: "Unknown error"
                _toastEvent.emit("Description updated, failed to update name: $nameMsg")
                onComplete(false)
            } else {
                val err = nameResult?.exceptionOrNull() ?: descResult?.exceptionOrNull()
                _toastEvent.emit(err?.toUserMessage(resourceProvider) ?: "Failed to update")
                onComplete(false)
            }
            } finally {
                _isSavingInfo.value = false
            }
        }
    }

    // Delete Playlist
    fun deletePlaylist(id: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(id).collect { result ->
                result.fold(
                    onSuccess = {
                        playlistMutationBus.emit(PlaylistMutationEvent.Deleted(id))
                        _toastEvent.emit("Playlist deleted")
                        onSuccess()
                    },
                    onFailure = { error ->
                        _toastEvent.emit(error.toUserMessage(resourceProvider))
                    }
                )
            }
        }
    }

    // ==================== AddSearch and Add ====================

    private val _addMusicSearchQuery = MutableStateFlow("")
    val addMusicSearchQuery: StateFlow<String> = _addMusicSearchQuery.asStateFlow()

    private val _addMusicSearchState = MutableStateFlow<AddMusicSearchState>(AddMusicSearchState.Idle)
    val addMusicSearchState: StateFlow<AddMusicSearchState> = _addMusicSearchState.asStateFlow()

    private var addMusicSearchJob: Job? = null

    fun updateAddMusicSearchQuery(query: String) {
        _addMusicSearchQuery.value = query
        addMusicSearchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _addMusicSearchState.value = AddMusicSearchState.Idle
            return
        }
        addMusicSearchJob = viewModelScope.launch {
            delay(300)
            _addMusicSearchState.value = AddMusicSearchState.Loading
            searchRepository.search(trimmed, SearchType.SONG, offset = 0, limit = 30).collect { result ->
                result.fold(
                    onSuccess = { pageResult ->
                        val tracks = pageResult.items.filterIsInstance<SearchResultItem.SongItem>().map { it.track }
                        _addMusicSearchState.value = AddMusicSearchState.Success(tracks)
                    },
                    onFailure = { error ->
                        _addMusicSearchState.value = AddMusicSearchState.Error(error.toUserMessage(resourceProvider))
                    }
                )
            }
        }
    }

    fun clearAddMusicSearch() {
        addMusicSearchJob?.cancel()
        _addMusicSearchQuery.value = ""
        _addMusicSearchState.value = AddMusicSearchState.Idle
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            playlistRepository.manipulatePlaylistTracks("add", playlistId, listOf(track.id)).collect { result ->
                result.onSuccess {
                    _toastEvent.emit("Added to playlist")
                    _uiState.update { state ->
                        if (state is PlaylistUiState.Success && state.playlist.id == playlistId) {
                            val updatedTracks = state.playlist.tracks.toMutableList().apply {
                                if (none { it.id == track.id }) {
                                    add(track)
                                }
                            }
                            state.copy(playlist = state.playlist.copy(tracks = updatedTracks, trackCount = updatedTracks.size))
                        } else state
                    }
                    onComplete(true)
                }.onFailure { e ->
                    _toastEvent.emit(e.toUserMessage(resourceProvider))
                    onComplete(false)
                }
            }
        }
    }

    // ==================== Save ====================

    fun updateTrackOrder(playlistId: Long, newTracks: List<Track>, onComplete: (Boolean) -> Unit) {
        // op=update ，PlaylistsClear
        if (newTracks.isEmpty()) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            playlistRepository.manipulatePlaylistTracks("update", playlistId, newTracks.map { it.id }).collect { result ->
                result.onSuccess {
                    _toastEvent.emit("Song order updated")
                    _uiState.update { state ->
                        if (state is PlaylistUiState.Success && state.playlist.id == playlistId) {
                            state.copy(playlist = state.playlist.copy(tracks = newTracks))
                        } else state
                    }
                    onComplete(true)
                }.onFailure { e ->
                    _toastEvent.emit(e.toUserMessage(resourceProvider))
                    onComplete(false)
                }
            }
        }
    }

    // DownloadPlaylists
    fun downloadPlaylist(playlistId: Long, playlistName: String, tracks: List<Track>, level: String) {
        if (tracks.isEmpty()) {
            viewModelScope.launch { _toastEvent.emit("No downloadable songs") }
            return
        }
        val downloadTracks = tracks.map { track ->
            DownloadTrackInfo(
                track.id, track.name, track.ar.joinToString("/") { it.name },
                track.al.name, track.al.picUrl.takeIf { it.isNotBlank() },
                yearFromEpochMillis(track.publishTime),
                videoId = YouTubeIdRegistry.getVideoId(track.id)
            )
        }
        songDownloadManager.enqueueBatch(
            downloadTracks, level, batchTag = "playlist_$playlistId", batchLabel = playlistName
        )
        viewModelScope.launch { _toastEvent.emit("Added ${downloadTracks.size} songs to download queue") }
    }

    // DownloadSongs
    fun downloadTrack(track: Track, level: String) {
        val downloadTrack = DownloadTrackInfo(
            track.id,
            track.name,
            track.ar.joinToString("/") { it.name },
            track.al.name,
            track.al.picUrl.takeIf { it.isNotBlank() },
            yearFromEpochMillis(track.publishTime),
            videoId = YouTubeIdRegistry.getVideoId(track.id)
        )
        songDownloadManager.enqueueSingle(downloadTrack, level)
        viewModelScope.launch { _toastEvent.emit("Added to download queue") }
    }
}

// AddSearch
sealed interface AddMusicSearchState {
    data object Idle : AddMusicSearchState
    data object Loading : AddMusicSearchState
    data class Success(val tracks: List<Track>) : AddMusicSearchState
    data class Error(val message: String) : AddMusicSearchState
}
