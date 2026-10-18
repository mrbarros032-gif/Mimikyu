package com.lin0721.linmusic.core.localmusic

import android.net.Uri

// Local Music
enum class LocalTrackSource { MIMIKYU_DOWNLOAD, EXTERNAL, IMPORTED }

data class LocalTrack(
    val mediaStoreId: Long,
    val songId: Long?,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long,
    val sizeBytes: Long,
    val uri: Uri,
    val path: String?,
    val dateAddedMs: Long,
    val source: LocalTrackSource
)
