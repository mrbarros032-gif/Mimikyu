package com.lin0721.linmusic.feature.localmusic.ui

import com.lin0721.linmusic.core.localmusic.LocalTrack

enum class LocalMusicGroupMode(val label: String) {
    SONGS("Tracks"), ARTIST("Artists"), ALBUM("Albums"), FOLDER("Folders")
}

enum class LocalMusicSortOrder(val label: String) {
    DATE_DESC("Recently Added"), NAME_ASC("By Name"), SIZE_DESC("By Size")
}

fun sortTracks(tracks: List<LocalTrack>, order: LocalMusicSortOrder): List<LocalTrack> = when (order) {
    LocalMusicSortOrder.DATE_DESC -> tracks.sortedByDescending { it.dateAddedMs }
    LocalMusicSortOrder.NAME_ASC -> tracks.sortedBy { it.title }
    LocalMusicSortOrder.SIZE_DESC -> tracks.sortedByDescending { it.sizeBytes }
}

data class LocalMusicGroup(val key: String, val tracks: List<LocalTrack>)

private const val UNKNOWN_ARTIST = "Unknown Artist"
private const val UNKNOWN_ALBUM = "Unknown Album"
private const val MIMIKYU_FOLDER_LABEL = "Mimikyu Downloads"

fun groupTracks(tracks: List<LocalTrack>, mode: LocalMusicGroupMode): List<LocalMusicGroup> {
    if (mode == LocalMusicGroupMode.SONGS) return emptyList()
    val grouped = when (mode) {
        LocalMusicGroupMode.ARTIST -> tracks.groupBy { it.artist.ifBlank { UNKNOWN_ARTIST } }
        LocalMusicGroupMode.ALBUM -> tracks.groupBy { it.album?.takeIf { a -> a.isNotBlank() } ?: UNKNOWN_ALBUM }
        LocalMusicGroupMode.FOLDER -> tracks.groupBy(::folderLabel)
        LocalMusicGroupMode.SONGS -> emptyMap()
    }
    return grouped.map { (key, list) -> LocalMusicGroup(key, list) }.sortedBy { it.key }
}


private fun folderLabel(track: LocalTrack): String {
    val path = track.path ?: return MIMIKYU_FOLDER_LABEL
    val parent = path.substringBeforeLast('/', "")
    return parent.substringAfterLast('/').ifBlank { "Root" }
}
