package com.lin0721.linmusic.core.contentfilter

import com.lin0721.linmusic.core.auth.UserPreferences
import kotlinx.coroutines.flow.first

// of "Block Artist"
class ContentFilter(private val userPreferences: UserPreferences) {

    suspend fun <T> filterBlockedArtists(items: List<T>, artistIds: (T) -> List<Long>): List<T> {
        val blocked = userPreferences.blockedArtistIds.first()
        if (blocked.isEmpty()) return items
        return items.filter { item -> artistIds(item).none { it in blocked } }
    }
}
