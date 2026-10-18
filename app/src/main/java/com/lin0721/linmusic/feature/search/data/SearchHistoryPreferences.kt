package com.lin0721.linmusic.feature.search.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lin0721.linmusic.core.log.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "SearchHistoryPreferences"
private const val MAX_HISTORY_SIZE = 20

private val Context.searchHistoryDataStore by preferencesDataStore(
    name = "search_history_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { ex ->
        AppLogger.e(TAG, "Search history corrupted, reset to default", ex)
        emptyPreferences()
    }
)

@kotlinx.serialization.Serializable
enum class RecentItemType {
    ARTIST, SONG, ALBUM, PLAYLIST, KEYWORD
}

@kotlinx.serialization.Serializable
data class RecentSearchItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val type: RecentItemType,
    val isVerified: Boolean = false,
    val isExplicit: Boolean = false,
    val rawId: Long = 0L,
    val artistName: String = ""
)

val DEFAULT_RECENT_SEARCH_ITEMS: List<RecentSearchItem> = emptyList()

private val PLACEHOLDER_RECENT_IDS = setOf(
    "artist_the_weeknd",
    "artist_drake",
    "song_love_me",
    "artist_taylor_swift",
    "artist_lana_del_rey",
    "artist_billie_eilish"
)

// Search history and recent items preferences
class SearchHistoryPreferences(private val context: Context) {

    companion object {
        private val KEY_HISTORY = stringPreferencesKey("search_history")
        private val KEY_RECENTS = stringPreferencesKey("search_recents_v2")
        private val json = Json { ignoreUnknownKeys = true }
    }

    val history: Flow<List<String>> = context.searchHistoryDataStore.data.map { prefs ->
        decode(prefs[KEY_HISTORY])
    }

    val recentItems: Flow<List<RecentSearchItem>> = context.searchHistoryDataStore.data.map { prefs ->
        val raw = prefs[KEY_RECENTS]
        if (raw == null) {
            emptyList()
        } else {
            (decodeRecentItems(raw) ?: emptyList()).filterNot { it.id in PLACEHOLDER_RECENT_IDS }
        }
    }

    suspend fun addKeyword(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return
        context.searchHistoryDataStore.edit { prefs ->
            val current = decode(prefs[KEY_HISTORY])
            val updated = (listOf(trimmed) + current.filterNot { it == trimmed }).take(MAX_HISTORY_SIZE)
            prefs[KEY_HISTORY] = json.encodeToString(updated)

            // Also keep recent items updated
            val currentRecents = (decodeRecentItems(prefs[KEY_RECENTS]) ?: emptyList()).filterNot { it.id in PLACEHOLDER_RECENT_IDS }
            val keywordItem = RecentSearchItem(
                id = "kw_$trimmed",
                title = trimmed,
                subtitle = "Search",
                imageUrl = "",
                type = RecentItemType.KEYWORD
            )
            val updatedRecents = (listOf(keywordItem) + currentRecents.filterNot { it.title.equals(trimmed, ignoreCase = true) }).take(MAX_HISTORY_SIZE)
            prefs[KEY_RECENTS] = json.encodeToString(updatedRecents)
        }
    }

    suspend fun addRecentItem(item: RecentSearchItem) {
        if (item.id in PLACEHOLDER_RECENT_IDS) return
        context.searchHistoryDataStore.edit { prefs ->
            val currentRecents = (decodeRecentItems(prefs[KEY_RECENTS]) ?: emptyList()).filterNot { it.id in PLACEHOLDER_RECENT_IDS }
            val updatedRecents = (listOf(item) + currentRecents.filterNot { it.id == item.id || it.title.equals(item.title, ignoreCase = true) }).take(MAX_HISTORY_SIZE)
            prefs[KEY_RECENTS] = json.encodeToString(updatedRecents)
        }
    }

    suspend fun removeKeyword(keyword: String) {
        context.searchHistoryDataStore.edit { prefs ->
            val current = decode(prefs[KEY_HISTORY])
            prefs[KEY_HISTORY] = json.encodeToString(current.filterNot { it == keyword })

            val currentRecents = (decodeRecentItems(prefs[KEY_RECENTS]) ?: emptyList()).filterNot { it.id in PLACEHOLDER_RECENT_IDS }
            val updatedRecents = currentRecents.filterNot { it.title.equals(keyword, ignoreCase = true) }
            prefs[KEY_RECENTS] = json.encodeToString(updatedRecents)
        }
    }

    suspend fun removeRecentItem(id: String) {
        context.searchHistoryDataStore.edit { prefs ->
            val currentRecents = (decodeRecentItems(prefs[KEY_RECENTS]) ?: emptyList()).filterNot { it.id in PLACEHOLDER_RECENT_IDS }
            val updatedRecents = currentRecents.filterNot { it.id == id }
            prefs[KEY_RECENTS] = json.encodeToString(updatedRecents)
        }
    }

    suspend fun clear() {
        context.searchHistoryDataStore.edit { prefs ->
            prefs.remove(KEY_HISTORY)
            prefs[KEY_RECENTS] = json.encodeToString(emptyList<RecentSearchItem>())
        }
    }

    private fun decode(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }
            .onFailure { AppLogger.w(TAG, "Search history deserialization failed", it) }
            .getOrDefault(emptyList())
    }

    private fun decodeRecentItems(raw: String?): List<RecentSearchItem>? {
        if (raw.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<List<RecentSearchItem>>(raw) }
            .onFailure { AppLogger.w(TAG, "Recent items deserialization failed", it) }
            .getOrNull()
    }
}
