package com.lin0721.linmusic.core.player.recommendation

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.metadata.cleanMetadataText
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.player.QueueItem
import com.lin0721.linmusic.core.player.data.PlaybackRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "SmartRecommendationEngine"

private val Context.recommendationDataStore by preferencesDataStore(name = "october_recommendation_prefs")

@Serializable
data class RecommendationProfile(
    // Map of artist name to affinity score (increased on >30s listens, decreased on <30s skips)
    val artistAffinity: Map<String, Float> = emptyMap(),
    // Set of recently skipped song IDs (<30s) to penalize
    val recentSkippedSongIds: List<Long> = emptyList(),
    // Set of highly preferred song IDs (>30s multiple times)
    val preferredSongIds: List<Long> = emptyList()
)

/**
 * Intelligent Next-Song Recommendation Engine.
 *
 * Implements Mimikyu-style contextual recommendation and bandit feedback:
 * 1. Automatically recommends the next song based on the current song seed + user listening history.
 * 2. If no history is available, seamlessly falls back to current track similarity graph.
 * 3. 30-Second Metric Rule:
 *    - Listened >= 30 seconds -> High preference (+2.0 artist affinity, +1.5 track preference).
 *    - Skipped < 30 seconds -> Low preference (-0.5 artist affinity, -3.0 track penalty, added to recent skips).
 */
class SmartRecommendationEngine(
    private val context: Context,
    private val repository: PlaybackRepository,
    private val tasteSignalScorer: TasteSignalScorer? = null,
    private val lastFmRepository: com.lin0721.linmusic.core.lastfm.LastFmRepository = com.lin0721.linmusic.core.lastfm.LastFmRepository()
) {
    companion object {
        private val KEY_PROFILE = stringPreferencesKey("recommendation_profile_json")
        private const val MAX_RECENT_SKIPS = 50
        private const val MIN_LISTEN_FOR_PREFERENCE_SEC = 30L
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Called whenever a track finishes playing or is skipped.
     * Analyzes listened duration to update recommendation weights.
     */
    suspend fun recordTrackPlaybackMetric(
        songId: Long,
        videoId: String = "",
        title: String = "",
        artist: String,
        coverUrl: String = "",
        listenedSeconds: Long,
        durationSeconds: Long
    ) {
        if (songId <= 0L || artist.isBlank()) return

        // Forward implicit user signals to TasteSignalScorer for 5-pillar Mimikyu ranking
        tasteSignalScorer?.recordPlayback(
            songId = songId,
            videoId = videoId,
            title = title,
            artist = artist,
            coverUrl = coverUrl,
            listenedSeconds = listenedSeconds,
            durationSeconds = durationSeconds
        )

        val isHighPreference = listenedSeconds >= MIN_LISTEN_FOR_PREFERENCE_SEC
        AppLogger.d(TAG, "Recording metric for songId=$songId ($artist): listened=${listenedSeconds}s, highPreference=$isHighPreference")

        try {
            context.recommendationDataStore.edit { prefs ->
                val currentProfileStr = prefs[KEY_PROFILE]
                val profile = if (currentProfileStr != null) {
                    runCatching { json.decodeFromString<RecommendationProfile>(currentProfileStr) }
                        .getOrDefault(RecommendationProfile())
                } else {
                    RecommendationProfile()
                }

                val currentAffinity = profile.artistAffinity[artist] ?: 0f
                val updatedAffinityMap = profile.artistAffinity.toMutableMap()
                val updatedSkips = profile.recentSkippedSongIds.toMutableList()
                val updatedPreferred = profile.preferredSongIds.toMutableList()

                if (isHighPreference) {
                    // Reward artist and track
                    val delta = if (durationSeconds > 0 && listenedSeconds >= (durationSeconds * 0.8f)) 3.0f else 2.0f
                    updatedAffinityMap[artist] = (currentAffinity + delta).coerceIn(-10f, 100f)
                    if (!updatedPreferred.contains(songId)) {
                        updatedPreferred.add(songId)
                        if (updatedPreferred.size > 100) updatedPreferred.removeAt(0)
                    }
                    updatedSkips.remove(songId)
                } else {
                    // Penalize: user skipped under 30 seconds
                    updatedAffinityMap[artist] = (currentAffinity - 0.5f).coerceIn(-10f, 100f)
                    if (!updatedSkips.contains(songId)) {
                        updatedSkips.add(songId)
                        if (updatedSkips.size > MAX_RECENT_SKIPS) updatedSkips.removeAt(0)
                    }
                }

                val newProfile = profile.copy(
                    artistAffinity = updatedAffinityMap,
                    recentSkippedSongIds = updatedSkips,
                    preferredSongIds = updatedPreferred
                )
                prefs[KEY_PROFILE] = json.encodeToString(newProfile)
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to update recommendation profile", e)
        }
    }

    /**
     * Determines the optimal next song to append to the playback queue.
     * Uses hybrid collaborative similarity + user affinity ranking.
     */
    suspend fun getNextRecommendedTrack(
        currentTrack: QueueItem,
        activeQueue: List<QueueItem>,
        recentHistoryTracks: List<Track> = emptyList()
    ): QueueItem? {
        val currentSongId = currentTrack.songId
        if (currentSongId <= 0L) return null

        val profile = loadProfile()
        val existingSongIds = activeQueue.map { it.songId }.toSet()
        val skippedIds = profile.recentSkippedSongIds.toSet()

        // Discover related artists via Last.fm for higher semantic accuracy
        val primaryArtist = currentTrack.artist.split(",", "/", "&").firstOrNull()?.trim() ?: currentTrack.artist
        val lastFmSimilarArtists = if (primaryArtist.isNotBlank()) {
            runCatching { lastFmRepository.getSimilarArtists(primaryArtist, limit = 10) }
                .getOrDefault(emptyList())
                .map { it.lowercase() }
                .toSet()
        } else emptySet()

        // 1. Fetch similarity candidates for the currently playing song
        val similarCandidates = mutableListOf<Track>()
        try {
            repository.getSimilarSongs(currentSongId).collect { result ->
                result.onSuccess { tracks ->
                    similarCandidates.addAll(tracks)
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to query similar songs for songId=$currentSongId", e)
        }

        // 2. Filter out active queue and heavily skipped tracks
        val eligibleCandidates = similarCandidates.filter { candidate ->
            candidate.id != currentSongId &&
            !existingSongIds.contains(candidate.id) &&
            !skippedIds.contains(candidate.id)
        }

        // 3. Fallback: if similarity returned no new candidates, check recent history for preferred artists
        val finalPool = if (eligibleCandidates.isNotEmpty()) {
            eligibleCandidates
        } else if (recentHistoryTracks.isNotEmpty()) {
            recentHistoryTracks.filter { it.id != currentSongId && !existingSongIds.contains(it.id) }
        } else {
            emptyList()
        }

        if (finalPool.isEmpty()) return null

        // 4. Rank candidates using composite metric:
        // Score = BaseSimilarityRank + LastFmSimilarityBonus + ArtistAffinityBonus + PreferenceBonus
        val scoredCandidates = finalPool.mapIndexed { index, track ->
            val artistName = track.ar.firstOrNull()?.name ?: ""
            val artistNameLower = artistName.lowercase()
            val affinity = profile.artistAffinity[artistName] ?: 0f
            val isPreferred = profile.preferredSongIds.contains(track.id)
            val isLastFmSimilar = lastFmSimilarArtists.contains(artistNameLower) ||
                    lastFmSimilarArtists.any { it.contains(artistNameLower) || artistNameLower.contains(it) }

            // Earlier similar results get higher base similarity weight
            val baseSimilarityWeight = (finalPool.size - index).toFloat()
            val affinityWeight = affinity * 1.5f
            val preferenceWeight = if (isPreferred) 5f else 0f
            val lastFmWeight = if (isLastFmSimilar) 4.5f else 0f

            val totalScore = baseSimilarityWeight + affinityWeight + preferenceWeight + lastFmWeight
            track to totalScore
        }

        // Pick highest scoring track
        val bestMatch = scoredCandidates.maxByOrNull { it.second }?.first ?: finalPool.first()
        val bestArtist = bestMatch.ar.joinToString("/") { it.name }

        AppLogger.d(TAG, "Selected recommended next song: ${bestMatch.name} by $bestArtist (id=${bestMatch.id})")

        return QueueItem(
            songId = bestMatch.id,
            title = bestMatch.name,
            artist = bestArtist,
            coverUrl = bestMatch.al.picUrl
        )
    }

    /**
     * Generates a Mimikyu-quality radio queue seeded by [seedTrack].
     * Uses InnerTube's native RDAMVM track radio graph + implicit user taste signals + Mimikyu polish pass.
     */
    suspend fun getRecommendedQueueForTrack(
        seedTrack: QueueItem,
        count: Int = 25
    ): List<QueueItem> {
        val seedSongId = seedTrack.songId
        if (seedSongId <= 0L && seedTrack.videoId.isNullOrBlank()) return emptyList()

        // 1. Resolve videoId if missing
        val videoId = seedTrack.videoId?.takeIf { it.isNotBlank() }
            ?: (if (seedSongId > 0) com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry.getVideoId(seedSongId) else null)
            ?: InnerTubeRadioEngine.resolveVideoId(seedSongId, seedTrack.title, seedTrack.artist)

        if (videoId.isNullOrBlank()) return emptyList()

        // 2. Query InnerTube native RDAMVM track radio
        val radioCandidates = InnerTubeRadioEngine.getRadioCandidates(videoId, count = 40)

        // 3. Retrieve familiar favorites (30% split)
        val familiar = tasteSignalScorer?.getFamiliarFavorites()?.map {
            QueueItem(
                songId = it.songId,
                title = it.title,
                artist = it.artist,
                coverUrl = it.coverUrl,
                videoId = it.videoId
            )
        }.orEmpty()

        // 4. Exclude current seed title to prevent immediate repetition
        val excludedTitles = setOf(com.lin0721.linmusic.core.metadata.normalizeTrackTitleForDeduplication(seedTrack.title))

        // 5. Apply the Mimikyu Polish (Max-2 artist rule, 70/30 familiarity split, title deduplication)
        val polished = MimikyuPolishFilter.applyPolish(
            novelCandidates = radioCandidates.filter { it.songId != seedSongId && it.videoId != videoId },
            familiarFavorites = familiar.filter { it.songId != seedSongId },
            targetCount = count,
            excludedRecentTitles = excludedTitles
        )

        return if (polished.isNotEmpty()) polished else radioCandidates.take(count)
    }

    private suspend fun loadProfile(): RecommendationProfile {
        return try {
            val jsonStr = context.recommendationDataStore.data.map { it[KEY_PROFILE] }.first()
            if (jsonStr != null) {
                json.decodeFromString<RecommendationProfile>(jsonStr)
            } else {
                RecommendationProfile()
            }
        } catch (e: Exception) {
            RecommendationProfile()
        }
    }
}
