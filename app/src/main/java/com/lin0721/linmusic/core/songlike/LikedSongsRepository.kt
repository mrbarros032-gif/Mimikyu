package com.lin0721.linmusic.core.songlike

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.lin0721.linmusic.core.auth.awaitTask
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "LikedSongsRepo"

interface LikedSongsRepository {
    val likedSongs: StateFlow<List<LikedSong>>
    val likedVideoIds: StateFlow<Set<String>>
    val likedSongIds: StateFlow<Set<Long>>
    val errorEvents: SharedFlow<String>

    suspend fun like(song: LikedSong): Result<Unit>
    suspend fun unlike(videoId: String): Result<Unit>
    suspend fun toggleLike(song: LikedSong): Result<Unit>
    suspend fun toggleLike(track: Track): Result<Unit>
    suspend fun toggleLike(songId: Long): Result<Unit>

    fun isLiked(videoId: String): Boolean
    fun isLiked(songId: Long): Boolean
    fun getCurrentUid(): String?

    fun startListening(uid: String)
    fun stopListening()
}

class LikedSongsRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userPreferences: com.lin0721.linmusic.core.auth.UserPreferences? = null
) : LikedSongsRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listenerRegistration: ListenerRegistration? = null
    private var currentListeningUid: String? = null
    private val initialReadLogged = AtomicBoolean(false)

    private val _likedSongs = MutableStateFlow<List<LikedSong>>(emptyList())
    override val likedSongs: StateFlow<List<LikedSong>> = _likedSongs.asStateFlow()

    override val likedVideoIds: StateFlow<Set<String>> = _likedSongs
        .map { list -> list.map { it.videoId }.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    override val likedSongIds: StateFlow<Set<Long>> = likedVideoIds
        .map { videoIds ->
            videoIds.mapNotNull { vid ->
                YouTubeIdRegistry.register(vid)
            }.toSet()
        }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    override val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    init {
        // Load cached liked tracks from local storage on startup for guest / offline mode
        if (userPreferences != null) {
            scope.launch {
                userPreferences.likedTracks.collect { tracks ->
                    if (getCurrentUid() == null) {
                        val songs = tracks.mapNotNull { it.toLikedSong() }
                        if (songs.isNotEmpty()) {
                            _likedSongs.value = songs.distinctBy { it.videoId }
                        }
                    }
                }
            }
        }

        // Observe FirebaseAuth lifecycle: start on sign-in, remove on sign-out; never run without a uid
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null && user.uid.isNotBlank()) {
                startListening(user.uid)
            } else {
                stopListening()
            }
        }
    }

    override fun getCurrentUid(): String? = auth.currentUser?.uid?.takeIf { it.isNotBlank() }

    @Synchronized
    override fun startListening(uid: String) {
        if (uid.isBlank()) {
            stopListening()
            return
        }
        if (currentListeningUid == uid && listenerRegistration != null) {
            return
        }

        stopListening()
        currentListeningUid = uid
        AppLogger.i(TAG, "Starting Firestore snapshot listener for user: $uid")

        val query = firestore.collection("users")
            .document(uid)
            .collection("likedSongs")
            .orderBy("likedAt", Query.Direction.DESCENDING)
            .limit(500)

        listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                AppLogger.e(TAG, "Snapshot listener error: ${error.message}", error)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener

            val docCount = snapshot.documents.size
            if (!initialReadLogged.getAndSet(true)) {
                AppLogger.i(TAG, "Initial liked songs read count for app launch: $docCount documents")
            } else {
                AppLogger.d(TAG, "Liked songs snapshot updated: $docCount documents")
            }

            val validSongs = snapshot.documents.mapNotNull { doc ->
                val videoId = doc.getString("videoId")?.takeIf { it.isNotBlank() } ?: doc.id.takeIf { it.isNotBlank() }
                val title = doc.getString("title")?.takeIf { it.isNotBlank() }

                // Skip any document missing videoId or title instead of crashing
                if (videoId == null || title == null) {
                    AppLogger.w(TAG, "Skipping invalid liked song document: ${doc.id}")
                    return@mapNotNull null
                }

                val artist = doc.getString("artist").orEmpty()
                val artistId = doc.getString("artistId")
                val album = doc.getString("album")
                val durationSec = doc.getLong("durationSec") ?: 0L
                val thumbnailUrl = doc.getString("thumbnailUrl").orEmpty()
                val likedAt = doc.getTimestamp("likedAt")

                YouTubeIdRegistry.register(
                    videoId = videoId,
                    title = title,
                    artist = artist,
                    durationSec = durationSec.toInt(),
                    album = album.orEmpty(),
                    coverUrl = thumbnailUrl
                )

                LikedSong(
                    videoId = videoId,
                    title = title,
                    artist = artist,
                    artistId = artistId,
                    album = album,
                    durationSec = durationSec,
                    thumbnailUrl = thumbnailUrl,
                    likedAt = likedAt
                )
            }

            val distinctSongs = validSongs.distinctBy { it.videoId }
            _likedSongs.value = distinctSongs
            userPreferences?.let { prefs ->
                scope.launch {
                    val tracks = distinctSongs.map { it.toTrack() }
                    prefs.saveLikedTracks(tracks)
                }
            }
        }
    }

    @Synchronized
    override fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
        currentListeningUid = null
        scope.launch {
            val localTracks = userPreferences?.likedTracks?.firstOrNull() ?: emptyList()
            _likedSongs.value = localTracks.mapNotNull { it.toLikedSong() }.distinctBy { it.videoId }
        }
        AppLogger.i(TAG, "Stopped Firestore snapshot listener and kept local state")
    }

    override suspend fun like(song: LikedSong): Result<Unit> {
        val cleanVid = YouTubeIdRegistry.sanitizeVideoId(song.videoId).ifBlank { song.videoId }
        val cleanSong = song.copy(videoId = cleanVid)
        if (cleanSong.videoId.isBlank() || cleanSong.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid song videoId or title"))
        }

        // Optimistic update: newly liked song added at front; duplicate overwrites in memory
        val previous = _likedSongs.value
        val updated = listOf(cleanSong) + previous.filter { it.videoId != cleanSong.videoId }
        _likedSongs.value = updated

        // Sync with local UserPreferences so offline/guest access works
        userPreferences?.let { prefs ->
            val existing = prefs.likedTracks.firstOrNull() ?: emptyList()
            val track = cleanSong.toTrack()
            val updatedTracks = if (existing.none { it.id == track.id || YouTubeIdRegistry.getVideoId(it.id) == cleanVid }) {
                listOf(track) + existing
            } else {
                existing
            }
            prefs.saveLikedTracks(updatedTracks)
        }

        val uid = getCurrentUid()
        if (uid == null) {
            // Guest mode: local persistence succeeded!
            AppLogger.d(TAG, "Liked song in guest/local mode: ${cleanSong.videoId} (${cleanSong.title})")
            return Result.success(Unit)
        }

        return try {
            val data = hashMapOf(
                "videoId" to cleanSong.videoId,
                "title" to cleanSong.title,
                "artist" to cleanSong.artist,
                "artistId" to cleanSong.artistId,
                "album" to cleanSong.album,
                "durationSec" to cleanSong.durationSec,
                "thumbnailUrl" to cleanSong.thumbnailUrl,
                "likedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("users")
                .document(uid)
                .collection("likedSongs")
                .document(cleanSong.videoId)
                .set(data)
                .awaitTask()

            AppLogger.d(TAG, "Successfully liked song to Firestore: ${cleanSong.videoId} (${cleanSong.title})")
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to write liked song to Firestore: ${cleanSong.videoId}", e)
            Result.success(Unit)
        }
    }

    override suspend fun unlike(videoId: String): Result<Unit> {
        val cleanVid = YouTubeIdRegistry.sanitizeVideoId(videoId).ifBlank { videoId }
        if (cleanVid.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid videoId"))
        }

        // Optimistic update: remove from in-memory state
        val previous = _likedSongs.value
        _likedSongs.value = previous.filter { it.videoId != cleanVid }

        // Sync with local UserPreferences
        userPreferences?.let { prefs ->
            val existing = prefs.likedTracks.firstOrNull() ?: emptyList()
            val updated = existing.filterNot { it.toLikedSong()?.videoId == cleanVid || YouTubeIdRegistry.getVideoId(it.id) == cleanVid }
            prefs.saveLikedTracks(updated)
        }

        val uid = getCurrentUid()
        if (uid == null) {
            // Guest mode: local remove succeeded!
            AppLogger.d(TAG, "Unliked song in guest/local mode: $cleanVid")
            return Result.success(Unit)
        }

        return try {
            firestore.collection("users")
                .document(uid)
                .collection("likedSongs")
                .document(cleanVid)
                .delete()
                .awaitTask()

            AppLogger.d(TAG, "Successfully unliked song from Firestore: $cleanVid")
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to delete liked song from Firestore: $cleanVid", e)
            Result.success(Unit)
        }
    }

    override suspend fun toggleLike(song: LikedSong): Result<Unit> {
        return if (isLiked(song.videoId)) {
            unlike(song.videoId)
        } else {
            like(song)
        }
    }

    override suspend fun toggleLike(track: Track): Result<Unit> {
        val vid = YouTubeIdRegistry.getVideoId(track.id) ?: track.id.toString()
        return if (isLiked(vid)) {
            unlike(vid)
        } else {
            val song = track.toLikedSong() ?: LikedSong(
                videoId = vid,
                title = track.name,
                artist = track.ar.firstOrNull()?.name.orEmpty(),
                album = track.al.name.takeIf { it.isNotBlank() },
                durationSec = track.dt / 1000L,
                thumbnailUrl = track.al.picUrl
            )
            like(song)
        }
    }

    override suspend fun toggleLike(songId: Long): Result<Unit> {
        val vid = YouTubeIdRegistry.getVideoId(songId)
        if (vid.isNullOrBlank()) {
            val msg = "Unable to resolve video ID for song"
            _errorEvents.emit(msg)
            return Result.failure(IllegalStateException(msg))
        }
        return if (isLiked(vid)) {
            unlike(vid)
        } else {
            val title = YouTubeIdRegistry.getTitle(songId)
            val artist = YouTubeIdRegistry.getArtist(songId)
            val album = YouTubeIdRegistry.getAlbumName(songId)
            val cover = YouTubeIdRegistry.getCoverUrl(songId)
            val dur = YouTubeIdRegistry.getDurationSec(songId).toLong()
            val song = LikedSong(
                videoId = vid,
                title = title.ifBlank { "Unknown Title" },
                artist = artist,
                album = album.takeIf { it.isNotBlank() },
                durationSec = dur,
                thumbnailUrl = cover
            )
            like(song)
        }
    }

    override fun isLiked(videoId: String): Boolean {
        val cleanVid = YouTubeIdRegistry.sanitizeVideoId(videoId).ifBlank { videoId }
        return cleanVid.isNotBlank() && cleanVid in likedVideoIds.value
    }

    override fun isLiked(songId: Long): Boolean {
        if (songId <= 0L) return false
        if (songId in likedSongIds.value) return true
        val vid = YouTubeIdRegistry.getVideoId(songId) ?: return false
        return isLiked(vid)
    }
}
