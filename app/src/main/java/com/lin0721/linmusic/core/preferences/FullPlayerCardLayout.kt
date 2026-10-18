package com.lin0721.linmusic.core.preferences

// Card identifiers for FullPlayer screen
enum class FullPlayerCard(val key: String, val title: String) {
    LYRICS("lyrics", "Lyrics"),
    SONG_DETAIL("song_detail", "Song Details"),
    MUSIC_MEMORY("music_memory", "Music Memory"),
    ABOUT_ARTIST("about_artist", "About Artist"),
    ARTIST_ALBUMS("artist_albums", "Artist Albums"),
    SIMILAR_ARTISTS("similar_artists", "Similar Artists");

    companion object {
        fun fromKey(key: String): FullPlayerCard? = entries.firstOrNull { it.key == key }
    }
}

data class FullPlayerCardSetting(
    val card: FullPlayerCard,
    val visible: Boolean
)

object FullPlayerCardLayout {

    val DEFAULT: List<FullPlayerCardSetting> =
        FullPlayerCard.entries.map { FullPlayerCardSetting(it, visible = true) }

    fun encode(layout: List<FullPlayerCardSetting>): String =
        normalize(layout).joinToString(",") { "${it.card.key}:${if (it.visible) 1 else 0}" }

    fun decode(raw: String?): List<FullPlayerCardSetting> {
        if (raw.isNullOrBlank()) return DEFAULT
        val parsed = raw.split(",").mapNotNull { token ->
            val parts = token.trim().split(":", limit = 2)
            val card = FullPlayerCard.fromKey(parts[0].trim()) ?: return@mapNotNull null
            FullPlayerCardSetting(card, visible = parts.getOrNull(1)?.trim() != "0")
        }
        return normalize(parsed)
    }

    private fun normalize(layout: List<FullPlayerCardSetting>): List<FullPlayerCardSetting> {
        val distinct = layout.distinctBy { it.card }
        val missing = FullPlayerCard.entries
            .filter { card -> distinct.none { it.card == card } }
            .map { FullPlayerCardSetting(it, visible = true) }
        return distinct + missing
    }
}
