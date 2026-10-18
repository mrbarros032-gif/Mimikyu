package com.lin0721.linmusic.core.localmusic

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.lin0721.linmusic.core.download.DownloadPreferences
import com.lin0721.linmusic.core.download.isDefaultDownloadDirectoryUri
import com.lin0721.linmusic.core.log.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import android.content.Intent
import android.net.Uri

private const val TAG = "LocalMusicRepository"

// Local Music and
class LocalMusicRepository(
    private val context: Context,
    private val downloadPreferences: DownloadPreferences,
    private val importedMusicPreferences: ImportedMusicPreferences
) {


    fun requiredPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, requiredPermission()) == PackageManager.PERMISSION_GRANTED

    // （）
    fun availableStorageBytes(): Long = runCatching {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        stat.availableBlocksLong * stat.blockSizeLong
    }.getOrDefault(0L)

    suspend fun getAllExistingUris(): Set<String> = withContext(Dispatchers.IO) {
        val mediaStoreUris = queryMediaStore().map { it.uri.toString() }.toSet()
        val importedUris = importedMusicPreferences.records.first().map { it.uriString }.toSet()
        mediaStoreUris + importedUris
    }

    // and
    suspend fun scan(): List<LocalTrack> = withContext(Dispatchers.IO) {
        val recordsByUri = downloadPreferences.records.first()
            .filter { isDefaultDownloadDirectoryUri(it.mediaStoreUri) }
            .associateBy { it.mediaStoreUri }

        val mediaStoreTracks = queryMediaStore().map { track ->
            val record = recordsByUri[track.uri.toString()]
            if (record != null) {
                val resolvedTitle = record.songName.takeIf { it.isNotBlank() } ?: track.title
                val resolvedArtist = record.artistName.takeIf { it.isNotBlank() } ?: track.artist
                track.copy(
                    songId = record.songId,
                    title = resolvedTitle,
                    artist = resolvedArtist,
                    source = LocalTrackSource.MIMIKYU_DOWNLOAD
                )
            } else {
                track
            }
        }

        val existingUris = mediaStoreTracks.map { it.uri.toString() }.toSet()
        val importedRecords = importedMusicPreferences.records.first()
        val validImportedTracks = mutableListOf<LocalTrack>()
        val invalidUris = mutableSetOf<String>()

        for (record in importedRecords) {
            if (record.uriString in existingUris) continue
            val uri = Uri.parse(record.uriString)
            val exists = runCatching {
                context.contentResolver.openInputStream(uri)?.use { true } ?: false
            }.getOrDefault(false)

            if (exists) {
                val stableId = record.uriString.hashCode().toLong().let { if (it == 0L) 1L else it }
                validImportedTracks.add(
                    LocalTrack(
                        mediaStoreId = stableId,
                        songId = null,
                        title = record.title,
                        artist = record.artist,
                        album = record.album,
                        durationMs = record.durationMs,
                        sizeBytes = record.sizeBytes,
                        uri = uri,
                        path = null,
                        dateAddedMs = record.dateAddedMs,
                        source = LocalTrackSource.IMPORTED
                    )
                )
            } else {
                invalidUris.add(record.uriString)
            }
        }

        if (invalidUris.isNotEmpty()) {
            importedMusicPreferences.removeRecords(invalidUris)
        }

        mediaStoreTracks + validImportedTracks
    }

    // Delete
    suspend fun delete(track: LocalTrack): Boolean = withContext(Dispatchers.IO) {
        if (track.source == LocalTrackSource.IMPORTED) {
            importedMusicPreferences.removeRecords(setOf(track.uri.toString()))
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    track.uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            return@withContext true
        }

        val deleted = runCatching { context.contentResolver.delete(track.uri, null, null) > 0 }
            .getOrDefault(false)
        if (deleted && track.songId != null) {
            downloadPreferences.removeRecord(track.songId)
        }
        deleted
    }

    private fun queryMediaStore(): List<LocalTrack> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val results = mutableListOf<LocalTrack>()
        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol) ?: "Unknown Track"
                    val rawArtist = cursor.getString(artistCol).orEmpty()
                    val (effectiveArtist, effectiveTitle) = if (
                        (rawArtist.isBlank() || rawArtist == "<unknown>" || (rawArtist == "Unknown Artist" || rawArtist == "Unknown Artist")) &&
                        rawTitle.contains(" - ")
                    ) {
                        val parts = rawTitle.split(" - ", limit = 2)
                        parts[0].trim().ifBlank { "Unknown Artist" } to parts[1].trim().ifBlank { rawTitle }
                    } else {
                        (rawArtist.ifBlank { "Unknown Artist" }) to rawTitle
                    }

                    results += LocalTrack(
                        mediaStoreId = id,
                        songId = null,
                        title = effectiveTitle,
                        artist = effectiveArtist,
                        album = cursor.getString(albumCol),
                        durationMs = cursor.getLong(durationCol),
                        sizeBytes = cursor.getLong(sizeCol),
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                        path = cursor.getString(dataCol),
                        dateAddedMs = cursor.getLong(dateCol) * 1000,
                        source = LocalTrackSource.EXTERNAL
                    )
                }
            }
        }.onFailure { AppLogger.e(TAG, "MediaStore audio scan failed", it) }
        return results
    }
}
