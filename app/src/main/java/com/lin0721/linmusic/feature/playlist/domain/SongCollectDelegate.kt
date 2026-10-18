package com.lin0721.linmusic.feature.playlist.domain

import com.lin0721.linmusic.core.auth.UserPreferences
import com.lin0721.linmusic.core.model.isLikedSongsPlaylist
import com.lin0721.linmusic.core.network.ResourceProvider
import com.lin0721.linmusic.core.network.toUserMessage
import com.lin0721.linmusic.core.songlike.SongLikeRepository
import com.lin0721.linmusic.core.ui.components.PlaylistCollectItem
import com.lin0721.linmusic.core.ui.components.PlaylistCollectState
import com.lin0721.linmusic.core.userplaylist.UserPlaylist
import com.lin0721.linmusic.core.userplaylist.UserPlaylistRepository
import com.lin0721.linmusic.feature.playlist.data.PlaylistRepository
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// “Save to Playlist” of  and ， artist/playlist
class SongCollectDelegate(
    private val userPlaylistRepository: UserPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val songLikeRepository: SongLikeRepository,
    private val createPlaylistAndAddSongUseCase: CreatePlaylistAndAddSongUseCase,
    private val userPreferences: UserPreferences,
    private val resourceProvider: ResourceProvider
) {

    companion object {
        // Playlists ( uid -> Playlists)
        private val userPlaylistsCache = ConcurrentHashMap<Long, List<UserPlaylist>>()
        // PlaylistsSongs ID  (Playlists id -> Songs ID Set)
        private val playlistTrackIdsCache = ConcurrentHashMap<Long, MutableSet<Long>>()

        fun clearCache() {
            userPlaylistsCache.clear()
            playlistTrackIdsCache.clear()
        }
    }

    private val _state = MutableStateFlow(PlaylistCollectState())
    val state: StateFlow<PlaylistCollectState> = _state.asStateFlow()

    // Current of PlaylistsSongs；Playlists
    suspend fun prepare(songId: Long, likedSongIds: Set<Long>, onToast: suspend (String) -> Unit) {
        val profile = userPreferences.userProfile.first() ?: return

        // of Playlists，
        val cachedPlaylists = userPlaylistsCache[profile.uid]
        val playlists = if (cachedPlaylists != null) {
            cachedPlaylists
        } else {
            _state.update { it.copy(songId = songId, isLoading = true, collectItems = emptyList()) }
            val result = userPlaylistRepository.getUserPlaylists(profile.uid).firstOrNull()
            val fetched = result?.getOrNull()?.filter { it.userId == profile.uid }
            if (fetched != null) {
                userPlaylistsCache[profile.uid] = fetched
                fetched
            } else {
                _state.update { it.copy(isLoading = false) }
                result?.exceptionOrNull()?.let { onToast(it.toUserMessage(resourceProvider)) }
                return
            }
        }

        // Playlists，“Liked Songs”，
        val initialItems = playlists.map { playlist ->
            val contains = if (isLikedSongsPlaylist(playlist.name, playlist.id, profile.uid)) {
                likedSongIds.contains(songId)
            } else {
                playlistTrackIdsCache[playlist.id]?.contains(songId) ?: false
            }
            PlaylistCollectItem(
                playlistId = playlist.id,
                playlistName = playlist.name,
                coverUrl = playlist.coverImgUrl,
                isInitiallyContains = contains,
                isContains = contains
            )
        }
        _state.update { it.copy(songId = songId, isLoading = false, collectItems = initialItems) }

        // Songs ID  of Playlists，Moments
        val unvisitedPlaylists = playlists.filter { playlist ->
            !isLikedSongsPlaylist(playlist.name, playlist.id, profile.uid) && !playlistTrackIdsCache.containsKey(playlist.id)
        }
        if (unvisitedPlaylists.isEmpty()) return

        coroutineScope {
            unvisitedPlaylists.forEach { playlist ->
                launch {
                    val detailResult = playlistRepository.getPlaylistDetail(playlist.id).firstOrNull()
                    val detail = detailResult?.getOrNull()
                    if (detail != null) {
                        val trackIds = Collections.synchronizedSet(detail.tracks.map { it.id }.toMutableSet())
                        playlistTrackIdsCache[playlist.id] = trackIds
                        if (trackIds.contains(songId)) {
                            _state.update { state ->
                                if (state.songId != songId) return@update state
                                val updated = state.collectItems.map { item ->
                                    if (item.playlistId == playlist.id) {
                                        item.copy(isInitiallyContains = true, isContains = true)
                                    } else {
                                        item
                                    }
                                }
                                state.copy(collectItems = updated)
                            }
                        }
                    }
                }
            }
        }
    }

    // ；Playlists，Playlists
    suspend fun save(
        songId: Long,
        items: List<PlaylistCollectItem>,
        likedSongIds: Set<Long>,
        onToast: suspend (String) -> Unit,
        onLikedChanged: (Set<Long>) -> Unit
    ) {
        val profile = userPreferences.userProfile.first()
        var liked = likedSongIds

        items.forEach { item ->
            if (item.isContains == item.isInitiallyContains) return@forEach

            if (profile != null && isLikedSongsPlaylist(item.playlistName, item.playlistId, profile.uid)) {
                songLikeRepository.likeSong(songId, item.isContains).firstOrNull()
                    ?.onSuccess {
                        liked = if (item.isContains) liked + songId else liked - songId
                        onLikedChanged(liked)
                    }
                    ?.onFailure { onToast(it.toUserMessage(resourceProvider)) }
            } else {
                val op = if (item.isContains) "add" else "del"
                playlistRepository.manipulatePlaylistTracks(op, item.playlistId, listOf(songId)).firstOrNull()
                    ?.onSuccess {
                        val cachedIds = playlistTrackIdsCache[item.playlistId]
                        if (cachedIds != null) {
                            if (item.isContains) cachedIds.add(songId) else cachedIds.remove(songId)
                        }
                    }
                    ?.onFailure { onToast(it.toUserMessage(resourceProvider)) }
            }
        }
        onToast("Playlist saved successfully")
    }

    // New PlaylistSongs，PlaylistsPlaylists
    suspend fun createAndAdd(
        name: String,
        songId: Long,
        likedSongIds: Set<Long>,
        onToast: suspend (String) -> Unit
    ) {
        createPlaylistAndAddSongUseCase(name, listOf(songId)).firstOrNull()
            ?.onSuccess {
                onToast("Playlist created and tracks added")
                val profile = userPreferences.userProfile.first()
                if (profile != null) {
                    userPlaylistsCache.remove(profile.uid)
                }
                prepare(songId, likedSongIds, onToast)
            }
            ?.onFailure { onToast(it.toUserMessage(resourceProvider)) }
    }
}
