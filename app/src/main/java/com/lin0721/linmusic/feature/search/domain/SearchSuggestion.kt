package com.lin0721.linmusic.feature.search.domain

// ：Details，
sealed interface SearchSuggestion {
    val text: String

    data class Keyword(override val text: String) : SearchSuggestion

    data class ArtistMatch(val id: Long, override val text: String, val avatarUrl: String) : SearchSuggestion

    data class AlbumMatch(val id: Long, override val text: String, val artistName: String) : SearchSuggestion

    data class SongMatch(
        val id: Long,
        override val text: String,
        val artistName: String,
        val coverUrl: String,
        val rawVideoId: String = ""
    ) : SearchSuggestion
}
