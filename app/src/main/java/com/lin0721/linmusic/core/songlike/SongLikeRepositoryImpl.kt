package com.lin0721.linmusic.core.songlike

import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.model.Album
import com.lin0721.linmusic.core.model.Artist
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.network.apiFlow
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import com.lin0721.linmusic.core.playlistmutation.PlaylistMutationBus
import com.lin0721.linmusic.core.playlistmutation.PlaylistMutationEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SongLikeRepositoryImpl(
    private val apiService: SongLikeApi,
    private val tasteSignalScorer: com.lin0721.linmusic.core.player.recommendation.TasteSignalScorer? = null,
    private val likedSongsRepository: LikedSongsRepository? = null,
    private val userPreferences: com.lin0721.linmusic.core.auth.UserPreferences? = null,
    private val playlistMutationBus: PlaylistMutationBus? = null
) : SongLikeRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _likedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    override val likedSongIds: StateFlow<Set<Long>> = _likedSongIds.asStateFlow()

    init {
        // Synchronize in-memory liked song IDs with LikedSongsRepository & UserPreferences
        likedSongsRepository?.let { repo ->
            scope.launch {
                repo.likedSongIds.collect { ids ->
                    _likedSongIds.value = _likedSongIds.value + ids
                }
            }
        }
        userPreferences?.let { prefs ->
            scope.launch {
                prefs.likedTracks.collect { tracks ->
                    val ids = tracks.map { it.id }.filter { it > 0L }.toSet()
                    if (ids.isNotEmpty()) {
                        _likedSongIds.value = _likedSongIds.value + ids
                    }
                }
            }
        }
    }

    override fun getLikedSongIds(uid: Long): Flow<Result<List<Long>>> = flow {
        val repoIds = likedSongsRepository?.likedSongIds?.value ?: emptySet()
        val localTracks = userPreferences?.likedTracks?.firstOrNull()?.map { it.id }?.toSet() ?: emptySet()
        val currentIds = (_likedSongIds.value + repoIds + localTracks).toList()

        // If running in app with LikedSongsRepository or UserPreferences, return current IDs
        if (likedSongsRepository != null || userPreferences != null) {
            _likedSongIds.value = currentIds.toSet()
            emit(Result.success(currentIds))
            return@flow
        }

        // Fallback to legacy API if running without local/Firestore repos (e.g. unit tests)
        apiFlow(
            request = { apiService.getLikedSongIds(LikeSongListRequest(uid = uid)) },
            isSuccess = { it.isSuccess },
            code = { it.code },
            transform = { it.ids }
        ).collect { result ->
            result.onSuccess { ids ->
                _likedSongIds.value = ids.toSet()
            }
            emit(result)
        }
    }

    override fun likeSong(songId: Long, like: Boolean): Flow<Result<Unit>> = flow {
        val previous = _likedSongIds.value
        _likedSongIds.value = if (like) previous + songId else previous - songId

        // If running in app environment (with LikedSongsRepository or UserPreferences)
        if (likedSongsRepository != null || userPreferences != null) {
            try {
                val cleanVid = YouTubeIdRegistry.getVideoId(songId)?.let { YouTubeIdRegistry.sanitizeVideoId(it) }
                    ?.ifBlank { null }
                    ?: if (songId > 0L) "track_$songId" else ""
                val title = YouTubeIdRegistry.getTitle(songId).ifBlank { "Song #$songId" }
                val artist = YouTubeIdRegistry.getArtist(songId)
                val album = YouTubeIdRegistry.getAlbumName(songId)
                val cover = YouTubeIdRegistry.getCoverUrl(songId)
                val durationSec = YouTubeIdRegistry.getDurationSec(songId).toLong()

                if (like) {
                    if (cleanVid.isNotBlank()) {
                        val likedSong = LikedSong(
                            videoId = cleanVid,
                            title = title,
                            artist = artist,
                            album = album.takeIf { it.isNotBlank() },
                            durationSec = durationSec,
                            thumbnailUrl = cover
                        )
                        likedSongsRepository?.like(likedSong)
                    }
                    userPreferences?.let { prefs ->
                        val existing = prefs.likedTracks.firstOrNull() ?: emptyList()
                        val track = Track(
                            id = songId,
                            name = title,
                            ar = if (artist.isNotBlank()) listOf(Artist(name = artist)) else emptyList(),
                            al = Album(name = album, picUrl = cover),
                            dt = durationSec * 1000L
                        )
                        if (existing.none { it.id == songId || (cleanVid.isNotBlank() && YouTubeIdRegistry.getVideoId(it.id) == cleanVid) }) {
                            prefs.saveLikedTracks(listOf(track) + existing)
                        }
                    }
                    if (title.isNotBlank() || artist.isNotBlank()) {
                        tasteSignalScorer?.recordLiked(
                            songId = songId,
                            videoId = cleanVid,
                            title = title,
                            artist = artist,
                            coverUrl = cover,
                            isLiked = true
                        )
                    }
                } else {
                    if (cleanVid.isNotBlank()) {
                        likedSongsRepository?.unlike(cleanVid)
                    }
                    userPreferences?.let { prefs ->
                        val existing = prefs.likedTracks.firstOrNull() ?: emptyList()
                        prefs.saveLikedTracks(existing.filterNot { it.id == songId || (cleanVid.isNotBlank() && YouTubeIdRegistry.getVideoId(it.id) == cleanVid) })
                    }
                    if (title.isNotBlank() || artist.isNotBlank()) {
                        tasteSignalScorer?.recordLiked(
                            songId = songId,
                            videoId = cleanVid,
                            title = title,
                            artist = artist,
                            coverUrl = cover,
                            isLiked = false
                        )
                    }
                }

                playlistMutationBus?.emit(PlaylistMutationEvent.Updated(0L))

                // Best-effort legacy sync, never failing user operation if NetEase is unauthenticated
                try {
                    apiService.likeSong(LikeSongRequest(trackId = songId, like = like))
                } catch (e: Throwable) {
                    AppLogger.d("SongLikeRepo", "Optional legacy like sync ignored: ${e.message}")
                }

                emit(Result.success(Unit))
                return@flow
            } catch (e: Exception) {
                AppLogger.e("SongLikeRepo", "Failed to update like song: $songId", e)
                _likedSongIds.value = previous
                emit(Result.failure(e))
                return@flow
            }
        }

        // Fallback for tests without LikedSongsRepository / UserPreferences
        apiFlow(
            request = { apiService.likeSong(LikeSongRequest(trackId = songId, like = like)) },
            isSuccess = { it.isSuccess },
            code = { it.code },
            transform = { Unit }
        ).collect { result ->
            result.onSuccess {
                val title = YouTubeIdRegistry.getTitle(songId)
                val artist = YouTubeIdRegistry.getArtist(songId)
                val cover = YouTubeIdRegistry.getCoverUrl(songId)
                val videoId = YouTubeIdRegistry.getVideoId(songId).orEmpty()
                if (title.isNotBlank() || artist.isNotBlank()) {
                    tasteSignalScorer?.recordLiked(
                        songId = songId,
                        videoId = videoId,
                        title = title,
                        artist = artist,
                        coverUrl = cover,
                        isLiked = like
                    )
                }
            }.onFailure {
                _likedSongIds.value = previous
            }
            emit(result)
        }
    }

    override fun syncLikedSongIds(ids: Set<Long>) {
        _likedSongIds.value = ids
    }
}
