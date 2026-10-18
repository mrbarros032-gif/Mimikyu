package com.lin0721.linmusic.core.download

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.TimeUnit

// DownloadSongs
data class DownloadTrackInfo(
    val songId: Long,
    val songName: String,
    val artistName: String,
    val albumName: String = "",
    val coverUrl: String? = null,
    val albumYear: Int = 0
)

// Download
data class DownloadProgressItem(val workId: UUID, val songName: String, val progress: Int)


fun yearFromEpochMillis(epochMillis: Long): Int {
    if (epochMillis <= 0) return 0
    if (epochMillis in 1000L..9999L) return epochMillis.toInt()
    val millis = if (epochMillis in 100_000_000L..99_999_999_999L) epochMillis * 1000L else epochMillis
    return runCatching {
        java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).year
    }.getOrDefault(0)
}

// SongsDownload
class SongDownloadManager(
    private val context: Context,
    private val downloadPreferences: DownloadPreferences
) {

    companion object {
        private const val TAG_DOWNLOAD = "song_download"
        private fun uniqueWorkName(songId: Long) = "song_download_$songId"
    }

    private val workManager get() = WorkManager.getInstance(context)

    suspend fun isDownloaded(songId: Long): Boolean = downloadPreferences.isDownloaded(songId)

    fun enqueueSingle(track: DownloadTrackInfo, level: String): UUID {
        val request = buildRequest(track, level)
        workManager.enqueueUniqueWork(uniqueWorkName(track.songId), ExistingWorkPolicy.REPLACE, request)
        return request.id
    }

    // Listen & Cache
    fun enqueueStreamCache(track: DownloadTrackInfo, level: String): UUID {
        val request = buildRequest(track, level, batchTag = "stream_cache", batchLabel = "Listen & Cache")
        workManager.enqueueUniqueWork(uniqueWorkName(track.songId), ExistingWorkPolicy.KEEP, request)
        return request.id
    }

    // Download
    fun enqueueBatch(tracks: List<DownloadTrackInfo>, level: String, batchTag: String, batchLabel: String): List<UUID> =
        tracks.map { track ->
            val request = buildRequest(track, level, batchTag = batchTag, batchLabel = batchLabel)
            workManager.enqueueUniqueWork(uniqueWorkName(track.songId), ExistingWorkPolicy.REPLACE, request)
            request.id
        }

    fun cancel(songId: Long) {
        workManager.cancelUniqueWork(uniqueWorkName(songId))
    }

    fun observeBatch(batchTag: String): Flow<List<WorkInfo>> = workManager.getWorkInfosByTagFlow(batchTag)

    // of Download
    fun observeActiveDownloads(): Flow<List<DownloadProgressItem>> =
        workManager.getWorkInfosByTagFlow(TAG_DOWNLOAD).map { infos ->
            infos.filter { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
                .map { info ->
                    DownloadProgressItem(
                        workId = info.id,
                        songName = info.progress.getString(SongDownloadWorker.KEY_PROGRESS_SONG_NAME) ?: "",
                        progress = info.progress.getInt(SongDownloadWorker.KEY_PROGRESS_PERCENT, 0)
                    )
                }
        }

    fun observeSingle(songId: Long): Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName(songId))

    private fun buildRequest(
        track: DownloadTrackInfo,
        level: String,
        batchTag: String? = null,
        batchLabel: String? = null
    ) = OneTimeWorkRequestBuilder<SongDownloadWorker>()
        .setInputData(
            SongDownloadWorker.buildInputData(
                track.songId, track.songName, track.artistName, level,
                track.albumName, track.coverUrl, track.albumYear, batchTag, batchLabel
            )
        )
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
        .addTag(TAG_DOWNLOAD)
        .apply { batchTag?.let { addTag(it) } }
        .build()
}
