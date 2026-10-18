package com.lin0721.linmusic.feature.home.domain

// ： +
data class HomeShelf(
    val blockCode: String,
    val title: String,
    // ， of
    val showRank: Boolean = false,
    val cards: List<HomeCard>
)

// of 。 Mimikyu Current of
sealed interface HomeCard {
    val id: Long
    val title: String
    val coverUrl: String

    // ，
    val caption: String

    data class Playlist(
        override val id: Long,
        override val title: String,
        override val coverUrl: String,
        override val caption: String,
        val trackCount: Int? = null,
        val isDailyMix: Boolean = false,
        val isGenreMix: Boolean = false,
        val isPinned: Boolean = false
    ) : HomeCard

    data class Album(
        override val id: Long,
        override val title: String,
        override val coverUrl: String,
        override val caption: String,
        val trackCount: Int? = null,
        val year: Int? = null,
        val isPinned: Boolean = false
    ) : HomeCard

    data class Song(
        override val id: Long,
        override val title: String,
        override val coverUrl: String,
        override val caption: String
    ) : HomeCard

    // 。id  id  key ，Play songId
    data class Voice(
        override val id: Long,
        val songId: Long,
        override val title: String,
        override val coverUrl: String,
        override val caption: String
    ) : HomeCard
}

// of
data class HomeBlockPage(
    val shelves: List<HomeShelf>,
    // of ， null
    val nextCursor: String?,
    val hasMore: Boolean
)
