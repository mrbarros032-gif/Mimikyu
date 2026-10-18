package com.lin0721.linmusic.feature.music.ui

import com.lin0721.linmusic.feature.music.domain.MusicStyle
import com.lin0721.linmusic.feature.music.domain.StyleContent
import com.lin0721.linmusic.feature.music.domain.StylePreference

// Genres。「 of 」， tagId
sealed interface StyleSelection {
    data object Preference : StyleSelection
    data class Style(val id: Long) : StyleSelection
}

// 「」tab UI
sealed interface MusicUiState {
    data object Loading : MusicUiState

    data class Success(val data: MusicFeedData) : MusicUiState

    data class Error(val message: String) : MusicUiState
}

data class MusicFeedData(
    val styles: List<MusicStyle> = emptyList(),
    // ，「 of 」
    val preferences: List<StylePreference> = emptyList(),
    val selection: StyleSelection = StyleSelection.Preference,
    // CurrentGenres of ，null 「All」
    val selectedChildId: Long? = null,
    val content: StyleContent? = null,
    // Genres，
    val isContentLoading: Boolean = false
) {
    val hasPreference: Boolean get() = preferences.isNotEmpty()

    // Current of  tagId：，Genres， of Genres
    val activeTagId: Long?
        get() = selectedChildId ?: when (val current = selection) {
            is StyleSelection.Style -> current.id
            StyleSelection.Preference -> preferences.maxByOrNull { it.ratio }?.id
        }

    val selectedStyle: MusicStyle?
        get() = (selection as? StyleSelection.Style)?.let { sel -> styles.firstOrNull { it.id == sel.id } }
}
