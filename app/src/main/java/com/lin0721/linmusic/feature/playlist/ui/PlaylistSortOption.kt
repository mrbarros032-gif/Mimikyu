package com.lin0721.linmusic.feature.playlist.ui

import com.lin0721.linmusic.core.model.Track
import java.text.Collator
import java.util.Locale

private val defaultCollator: Collator = Collator.getInstance(Locale.getDefault())

enum class PlaylistSortOption(val label: String) {
    DEFAULT("Sort"),
    TITLE("Title"),
    ARTIST("Artist"),
    DURATION("Duration");

    fun sort(tracks: List<Track>): List<Track> = when (this) {
        DEFAULT -> tracks
        TITLE -> tracks.sortedWith(compareBy(defaultCollator) { it.name })
        ARTIST -> tracks.sortedWith(compareBy(defaultCollator) { it.ar.firstOrNull()?.name.orEmpty() })
        DURATION -> tracks.sortedBy { it.dt }
    }
}
