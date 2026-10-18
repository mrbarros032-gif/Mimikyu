package com.lin0721.linmusic.feature.listendata.data

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lin0721.linmusic.core.auth.AuthState
import com.lin0721.linmusic.core.history.PlaybackHistoryRepository
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import com.lin0721.linmusic.feature.listendata.domain.DayDuration
import com.lin0721.linmusic.feature.listendata.domain.ListenReport
import com.lin0721.linmusic.feature.listendata.domain.RankSong
import com.lin0721.linmusic.feature.listendata.domain.SongRank
import com.lin0721.linmusic.feature.listendata.domain.TodaySong
import com.lin0721.linmusic.feature.listendata.domain.TopArtist
import com.lin0721.linmusic.feature.listendata.domain.YearStat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

const val PERIOD_WEEK = "week"
const val PERIOD_MONTH = "month"

private const val TAG = "ListenDataRepo"

class ListenDataRepositoryImpl(
    private val authState: AuthState,
    private val playbackHistoryRepository: PlaybackHistoryRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ListenDataRepository {

    override fun getTotalDuration(): Flow<Result<Long>> = flow {
        val uid = authState.currentUid
        var firestoreSeconds = 0L
        if (!uid.isNullOrBlank()) {
            try {
                val doc = firestore.collection("users").document(uid)
                    .collection("stats").document("summary")
                    .get().await()
                firestoreSeconds = doc.getLong("totalSeconds") ?: 0L
            } catch (e: Exception) {
                AppLogger.w(TAG, "Error fetching totalDuration from Firestore: ${e.message}")
            }
        }
        val localSeconds = playbackHistoryRepository.plays.value.sumOf {
            if (it.listenedSec > 0) it.listenedSec else it.durationSec.coerceAtMost(300L)
        }
        emit(Result.success(maxOf(firestoreSeconds, localSeconds)))
    }

    override fun getTodaySongs(): Flow<Result<List<TodaySong>>> = flow {
        val todayPlays = playbackHistoryRepository.getTodaySongs()
        val mapped = todayPlays.map { play ->
            TodaySong(
                id = play.track.id,
                name = play.title,
                artistName = play.artist,
                coverUrl = play.coverUrl
            )
        }
        emit(Result.success(mapped))
    }

    override fun getSongRank(period: String): Flow<Result<SongRank>> = flow {
        val uid = authState.currentUid
        var songs: List<RankSong> = emptyList()

        if (!uid.isNullOrBlank()) {
            try {
                val snapshot = firestore.collection("users").document(uid)
                    .collection("topSongs")
                    .orderBy("plays", Query.Direction.DESCENDING)
                    .limit(10)
                    .get().await()

                songs = snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        val videoId = doc.id
                        val title = doc.getString("title").orEmpty()
                        val artist = doc.getString("artist").orEmpty()
                        val cover = doc.getString("thumbnailUrl").orEmpty()
                        val plays = doc.getLong("plays")?.toInt() ?: 0
                        val songId = YouTubeIdRegistry.register(videoId, title, artist, coverUrl = cover)
                        RankSong(
                            id = songId,
                            name = title,
                            artistName = artist,
                            coverUrl = cover,
                            playCount = plays
                        )
                    }.getOrNull()
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Firestore topSongs query failed, falling back to local history: ${e.message}")
            }
        }

        // Fallback: compute from local plays if Firestore returned empty or failed
        if (songs.isEmpty()) {
            val cutoff = System.currentTimeMillis() - if (period == PERIOD_WEEK) 7L * 86400000L else 30L * 86400000L
            val periodPlays = playbackHistoryRepository.plays.value.filter { it.playTime >= cutoff }
            songs = periodPlays
                .groupBy { (it.title.ifBlank { it.videoId }) to it.artist }
                .map { (_, group) ->
                    val first = group.first()
                    RankSong(
                        id = first.track.id,
                        name = first.title,
                        artistName = first.artist,
                        coverUrl = first.coverUrl,
                        playCount = group.size
                    )
                }
                .sortedByDescending { it.playCount }
                .take(10)
        }

        emit(Result.success(SongRank(totalCount = songs.size, songs = songs)))
    }

    override fun getReport(period: String): Flow<Result<ListenReport>> = flow {
        val uid = authState.currentUid
        var hasFirestoreData = false
        var playMinutes = 0
        var totalPlays = 0
        var listenDays = 0
        var days: List<DayDuration> = emptyList()
        var topArtists: List<TopArtist> = emptyList()

        if (!uid.isNullOrBlank()) {
            try {
                val limit = if (period == PERIOD_WEEK) 7L else 30L
                val dailySnapshot = firestore.collection("users").document(uid)
                    .collection("statsDaily")
                    .limit(limit)
                    .get().await()

                val artistSnapshot = firestore.collection("users").document(uid)
                    .collection("topArtists")
                    .orderBy("plays", Query.Direction.DESCENDING)
                    .limit(10)
                    .get().await()

                val dailyDocs = dailySnapshot.documents.sortedByDescending { it.id }
                if (dailyDocs.isNotEmpty()) {
                    hasFirestoreData = true
                    val totalSeconds = dailyDocs.sumOf { it.getLong("seconds") ?: 0L }
                    totalPlays = dailyDocs.sumOf { it.getLong("plays")?.toInt() ?: 0 }
                    playMinutes = (totalSeconds / 60L).toInt()
                    listenDays = dailyDocs.count { (it.getLong("seconds") ?: 0L) > 0L }

                    days = dailyDocs.reversed().map { doc ->
                        val dayKey = doc.id
                        val dayMinutes = ((doc.getLong("seconds") ?: 0L) / 60L).toInt()
                        DayDuration(
                            label = dayKey.substringAfterLast("-"),
                            fullLabel = dayKey,
                            minutes = dayMinutes
                        )
                    }
                }

                topArtists = artistSnapshot.documents.mapNotNull { doc ->
                    runCatching {
                        val name = doc.getString("name").orEmpty()
                        val plays = doc.getLong("plays")?.toInt() ?: 0
                        if (name.isBlank() && plays == 0) null else {
                            val artistId = (doc.id.hashCode().toLong() and 0x7FFFFFFFL)
                            TopArtist(
                                id = artistId,
                                name = name,
                                coverUrl = "",
                                countText = "$plays plays"
                            )
                        }
                    }.getOrNull()
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Firestore report query failed, falling back to local history: ${e.message}")
            }
        }

        // Fallback: compute from local plays
        if (!hasFirestoreData) {
            val cutoff = System.currentTimeMillis() - if (period == PERIOD_WEEK) 7L * 86400000L else 30L * 86400000L
            val periodPlays = playbackHistoryRepository.plays.value.filter { it.playTime >= cutoff }
            val totalSeconds = periodPlays.sumOf {
                if (it.listenedSec > 0) it.listenedSec else it.durationSec.coerceAtMost(300L)
            }
            playMinutes = (totalSeconds / 60L).toInt()
            totalPlays = periodPlays.size
            listenDays = periodPlays.map { it.dayKey }.distinct().size

            days = periodPlays.groupBy { it.dayKey }.map { (dayKey, dayGroup) ->
                val daySec = dayGroup.sumOf { if (it.listenedSec > 0) it.listenedSec else it.durationSec.coerceAtMost(300L) }
                DayDuration(
                    label = dayKey.substringAfterLast("-"),
                    fullLabel = dayKey,
                    minutes = (daySec / 60L).toInt()
                )
            }.sortedBy { it.fullLabel }

            if (topArtists.isEmpty()) {
                topArtists = periodPlays
                    .filter { it.artist.isNotBlank() && it.artist != "Unknown Artist" }
                    .groupBy { it.artist }
                    .map { (artist, group) ->
                        TopArtist(
                            id = (artist.hashCode().toLong() and 0x7FFFFFFFL),
                            name = artist,
                            coverUrl = group.firstOrNull()?.coverUrl.orEmpty(),
                            countText = "${group.size} plays"
                        )
                    }
                    .sortedByDescending { it.countText.substringBefore(" ").toIntOrNull() ?: 0 }
                    .take(10)
            }
        }

        val report = ListenReport(
            playMinutes = playMinutes,
            beyondPercentText = if (playMinutes > 0) "Mimikyu Listener" else "",
            achievementTitle = if (playMinutes > 0) "Music Enthusiast" else "",
            achievementSubtitle = if (playMinutes > 0) "$playMinutes mins listened" else "",
            listenDays = listenDays,
            wallpaperUrls = emptyList(),
            songCount = totalPlays,
            highlights = emptyList(),
            dailyDurations = days,
            timePeriods = emptyList(),
            topArtists = topArtists,
            genreName = "",
            ageLabel = "",
            languageName = "",
            vipItems = emptyList(),
            podcastMinutes = 0,
            podcastEpisodes = 0,
            friendsListening = emptyList(),
            friendKeywords = emptyList()
        )
        emit(Result.success(report))
    }

    override fun getYearStats(): Flow<Result<List<YearStat>>> = flow {
        val uid = authState.currentUid
        var stats: List<YearStat> = emptyList()

        if (!uid.isNullOrBlank()) {
            try {
                val snapshot = firestore.collection("users").document(uid)
                    .collection("statsMonthly")
                    .limit(12)
                    .get().await()

                val docs = snapshot.documents.sortedByDescending { it.id }
                stats = docs.mapNotNull { doc ->
                    runCatching {
                        val monthKey = doc.id // "yyyy-MM"
                        val plays = doc.getLong("plays")?.toInt() ?: 0
                        val seconds = doc.getLong("seconds") ?: 0L
                        val year = monthKey.substringBefore("-").toIntOrNull() ?: 2026
                        YearStat(
                            year = year,
                            playCount = plays,
                            durationSeconds = seconds,
                            label = monthKey
                        )
                    }.getOrNull()
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Firestore yearStats query failed, falling back to local history: ${e.message}")
            }
        }

        // Fallback: compute from local plays
        if (stats.isEmpty()) {
            val allPlays = playbackHistoryRepository.plays.value
            val monthlyGroup = allPlays.groupBy { it.dayKey.take(7) }
            stats = monthlyGroup.map { (monthKey, group) ->
                val seconds = group.sumOf { if (it.listenedSec > 0) it.listenedSec else it.durationSec.coerceAtMost(300L) }
                val year = monthKey.substringBefore("-").toIntOrNull() ?: 2026
                YearStat(
                    year = year,
                    playCount = group.size,
                    durationSeconds = seconds,
                    label = monthKey
                )
            }.sortedByDescending { it.label }
        }

        emit(Result.success(stats))
    }
}
