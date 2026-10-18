package com.lin0721.linmusic.core.player.recommendation

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lin0721.linmusic.core.log.AppLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "TasteSignalScorer"

private val Context.tasteSignalsDataStore by preferencesDataStore(name = "mimikyu_taste_signals_prefs")

@Serializable
data class ScoredTrackRecord(
    val songId: Long,
    val videoId: String = "",
    val title: String,
    val artist: String,
    val coverUrl: String = "",
    val score: Int = 0,
    val playCount: Int = 0,
    val lastPlayedTime: Long = System.currentTimeMillis(),
    val excludedUntilTime: Long = 0L,
    val isLiked: Boolean = false,
    val isSearched: Boolean = false,
    val genres: List<String> = emptyList()
)

@Serializable
data class TasteSignalsState(
    val tracks: Map<String, ScoredTrackRecord> = emptyMap() // key: songId or normalized title+artist
)

/**
 * Weights implicit user interactions to rank tracks and generate high-signal seeds.
 *
 * Scoring Rubric:
 * - Skipped in < 30s: -3 points + 14-day temporary exclusion from recommendation seeds.
 * - Full listen (> 80% duration): +1 point (eligible as basic seed).
 * - Replayed within the same session: +3 points (high priority seed for "On Repeat" & radio).
 * - Liked / Added to Playlist: +5 points (anchor seed for long-term taste clustering).
 * - Searched explicitly by name: +4 points (strong intent signal).
 */
class TasteSignalScorer(private val context: Context) {

    companion object {
        private val KEY_STATE = stringPreferencesKey("taste_signals_state_json")
        private const val FOURTEEN_DAYS_MS = 14L * 24 * 60 * 60 * 1000L
        private const val MAX_STORED_TRACKS = 250
    }

    private val json = Json { ignoreUnknownKeys = true }

    // Session playback tracker to detect in-session replays (cleared on app process restart)
    private val sessionPlayCounts = ConcurrentHashMap<String, Int>()

    /**
     * Records a playback completion or skip metric.
     */
    suspend fun recordPlayback(
        songId: Long,
        videoId: String = "",
        title: String,
        artist: String,
        coverUrl: String = "",
        listenedSeconds: Long,
        durationSeconds: Long
    ) {
        if (title.isBlank()) return
        val key = buildKey(songId, title, artist)

        // Track in-session replays
        val previousSessionPlays = sessionPlayCounts.getOrDefault(key, 0)
        sessionPlayCounts[key] = previousSessionPlays + 1
        val isSessionReplay = previousSessionPlays >= 1

        val isSkip = listenedSeconds < 30L
        val isFullListen = durationSeconds > 0 && listenedSeconds >= (durationSeconds * 0.8f)

        var delta = 0
        var exclusionTime = 0L

        if (isSkip) {
            delta -= 3
            exclusionTime = System.currentTimeMillis() + FOURTEEN_DAYS_MS
            AppLogger.d(TAG, "Penalized skip for '$title': -3 points, excluded for 14 days")
        } else {
            if (isFullListen) {
                delta += 1
            }
            if (isSessionReplay) {
                delta += 3
                AppLogger.d(TAG, "Rewarded session replay for '$title': +3 points")
            }
        }

        updateTrack(key, songId, videoId, title, artist, coverUrl) { existing ->
            val newScore = (existing?.score ?: 0) + delta
            val newPlayCount = (existing?.playCount ?: 0) + 1
            val finalExclusion = if (isSkip) exclusionTime else (existing?.excludedUntilTime ?: 0L)
            (existing ?: ScoredTrackRecord(
                songId = songId,
                videoId = videoId,
                title = title,
                artist = artist,
                coverUrl = coverUrl
            )).copy(
                score = newScore,
                playCount = newPlayCount,
                lastPlayedTime = System.currentTimeMillis(),
                excludedUntilTime = finalExclusion
            )
        }
    }

    /**
     * Records a track like or add-to-playlist action (+5 points anchor).
     */
    suspend fun recordLiked(
        songId: Long,
        videoId: String = "",
        title: String,
        artist: String,
        coverUrl: String = "",
        isLiked: Boolean
    ) {
        if (title.isBlank()) return
        val key = buildKey(songId, title, artist)
        val delta = if (isLiked) 5 else -5

        updateTrack(key, songId, videoId, title, artist, coverUrl) { existing ->
            val newScore = (existing?.score ?: 0) + delta
            (existing ?: ScoredTrackRecord(
                songId = songId,
                videoId = videoId,
                title = title,
                artist = artist,
                coverUrl = coverUrl
            )).copy(
                score = newScore,
                isLiked = isLiked,
                // Liking immediately cancels any temporary skip exclusion
                excludedUntilTime = if (isLiked) 0L else (existing?.excludedUntilTime ?: 0L)
            )
        }
    }

    /**
     * Records an explicit search intent (+4 points).
     */
    suspend fun recordSearched(
        songId: Long,
        videoId: String = "",
        title: String,
        artist: String,
        coverUrl: String = ""
    ) {
        if (title.isBlank()) return
        val key = buildKey(songId, title, artist)

        updateTrack(key, songId, videoId, title, artist, coverUrl) { existing ->
            val newScore = (existing?.score ?: 0) + 4
            (existing ?: ScoredTrackRecord(
                songId = songId,
                videoId = videoId,
                title = title,
                artist = artist,
                coverUrl = coverUrl
            )).copy(
                score = newScore,
                isSearched = true,
                excludedUntilTime = 0L // Explicit search overrides exclusion
            )
        }
    }

    /**
     * Retrieves valid candidate seed tracks with positive scores and no active 14-day exclusion.
     */
    suspend fun getEligibleSeedTracks(): List<ScoredTrackRecord> {
        val state = loadState()
        val now = System.currentTimeMillis()
        return state.tracks.values
            .filter { it.score > 0 && it.excludedUntilTime <= now }
            .sortedByDescending { it.score }
    }

    /**
     * Retrieves high-score tracks played in the past, but not played within the last [excludePlayedInLastDays] days.
     * Used for the 70/30 Familiarity Split.
     */
    suspend fun getFamiliarFavorites(
        minScore: Int = 2,
        excludePlayedInLastDays: Int = 3
    ): List<ScoredTrackRecord> {
        val state = loadState()
        val now = System.currentTimeMillis()
        val cutoffMs = now - (excludePlayedInLastDays.toLong() * 24 * 60 * 60 * 1000L)

        return state.tracks.values
            .filter { it.score >= minScore && it.excludedUntilTime <= now && it.lastPlayedTime < cutoffMs }
            .sortedByDescending { it.score }
    }

    /**
     * Retrieves tracks frequently replayed or high-scoring for the "On Repeat" shelf.
     */
    suspend fun getOnRepeatTracks(limit: Int = 20): List<ScoredTrackRecord> {
        val state = loadState()
        val now = System.currentTimeMillis()
        return state.tracks.values
            .filter { it.score >= 3 && it.playCount >= 2 && it.excludedUntilTime <= now }
            .sortedByDescending { it.score * 2 + it.playCount }
            .take(limit)
    }

    private suspend fun updateTrack(
        key: String,
        songId: Long,
        videoId: String,
        title: String,
        artist: String,
        coverUrl: String,
        transform: (ScoredTrackRecord?) -> ScoredTrackRecord
    ) {
        try {
            context.tasteSignalsDataStore.edit { prefs ->
                val currentStr = prefs[KEY_STATE]
                val state = if (currentStr != null) {
                    runCatching { json.decodeFromString<TasteSignalsState>(currentStr) }
                        .getOrDefault(TasteSignalsState())
                } else {
                    TasteSignalsState()
                }

                val existing = state.tracks[key]
                val updated = transform(existing)
                val newMap = state.tracks.toMutableMap()
                newMap[key] = updated

                // Trim to MAX_STORED_TRACKS (evict oldest low-scoring tracks)
                if (newMap.size > MAX_STORED_TRACKS) {
                    val evictKeys = newMap.entries
                        .sortedBy { it.value.score + (it.value.lastPlayedTime / 100000000) }
                        .take(newMap.size - MAX_STORED_TRACKS)
                        .map { it.key }
                    evictKeys.forEach { newMap.remove(it) }
                }

                prefs[KEY_STATE] = json.encodeToString(TasteSignalsState(newMap))
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to persist track taste signal for $title", e)
        }
    }

    private suspend fun loadState(): TasteSignalsState {
        return try {
            val str = context.tasteSignalsDataStore.data.map { it[KEY_STATE] }.first()
            if (str != null) json.decodeFromString<TasteSignalsState>(str) else TasteSignalsState()
        } catch (e: Exception) {
            TasteSignalsState()
        }
    }

    private fun buildKey(songId: Long, title: String, artist: String): String {
        return if (songId > 0) songId.toString() else "${title.trim().lowercase()}|${artist.trim().lowercase()}"
    }
}
