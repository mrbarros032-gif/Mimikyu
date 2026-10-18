package com.lin0721.linmusic.core.history

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.lin0721.linmusic.core.auth.AuthState
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.model.Album
import com.lin0721.linmusic.core.model.Artist
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import com.lin0721.linmusic.feature.recent.domain.RecentSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single shared repository and snapshot listener for Recent Plays and Today's Plays.
 * Listens to users/{uid}/plays (ordered by startedAt DESC, limit 100).
 * Prevents duplicate listeners and ensures optimal read counts.
 */
class PlaybackHistoryRepository(
    private val authState: AuthState,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "PlaybackHistoryRepo"
        const val MAX_PLAYS = 100
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _plays = MutableStateFlow<List<RecentSong>>(emptyList())
    val plays: StateFlow<List<RecentSong>> = _plays.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var activeListener: ListenerRegistration? = null
    private var lastListeningUid: String? = null

    init {
        scope.launch {
            authState.user.collect { user ->
                val uid = user?.uid
                if (uid != lastListeningUid) {
                    lastListeningUid = uid
                    setupListener(uid)
                }
            }
        }
    }

    @Synchronized
    private fun setupListener(uid: String?) {
        activeListener?.remove()
        activeListener = null

        if (uid.isNullOrBlank()) {
            _plays.value = emptyList()
            _isLoading.value = false
            return
        }

        _isLoading.value = true
        AppLogger.i(TAG, "Attaching single shared listener on users/$uid/plays (limit $MAX_PLAYS)")
        val query = firestore.collection("users").document(uid).collection("plays")
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .limit(MAX_PLAYS.toLong())

        activeListener = query.addSnapshotListener { snapshot, error ->
            _isLoading.value = false
            if (error != null) {
                AppLogger.e(TAG, "Error in plays snapshot listener: ${error.message}", error)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val list = snapshot.documents.mapNotNull { doc ->
                runCatching {
                    val id = doc.id
                    val videoId = doc.getString("videoId").orEmpty().ifBlank { id }
                    val title = doc.getString("title").orEmpty()
                    if (title.isBlank() && videoId.isBlank()) return@runCatching null

                    val artist = doc.getString("artist").orEmpty().ifBlank { "Unknown Artist" }
                    val album = doc.getString("album")
                    val cover = doc.getString("thumbnailUrl") ?: YouTubeIdRegistry.getCoverUrl(
                        YouTubeIdRegistry.register(videoId, title, artist)
                    )
                    val durationSec = doc.getLong("durationSec") ?: 0L
                    val listenedSec = doc.getLong("listenedSec") ?: 0L
                    val startedAtTimestamp = doc.getTimestamp("startedAt")
                    val playTime = startedAtTimestamp?.toDate()?.time ?: System.currentTimeMillis()
                    val dayKey = doc.getString("dayKey") ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(playTime))
                    val playedAtText = timeFormat.format(Date(playTime))

                    val songId = YouTubeIdRegistry.register(videoId, title, artist, durationSec.toInt(), album.orEmpty(), cover)
                    val track = Track(
                        id = songId,
                        name = title,
                        ar = listOf(Artist(id = 0L, name = artist)),
                        al = Album(id = 0L, name = album.orEmpty(), picUrl = cover),
                        dt = durationSec * 1000L
                    )

                    RecentSong(
                        id = id,
                        videoId = videoId,
                        title = title,
                        artist = artist,
                        album = album,
                        coverUrl = cover,
                        durationSec = durationSec,
                        listenedSec = listenedSec,
                        playTime = playTime,
                        dayKey = dayKey,
                        playedAtText = playedAtText,
                        track = track
                    )
                }.getOrNull()
            }
            _plays.value = list
            AppLogger.d(TAG, "Updated shared plays snapshot: ${list.size} tracks")
        }
    }

    fun getRecentSongs(): StateFlow<List<RecentSong>> = plays

    fun getTodaySongs(): List<RecentSong> {
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return _plays.value.filter { it.dayKey == todayKey }
    }
}
