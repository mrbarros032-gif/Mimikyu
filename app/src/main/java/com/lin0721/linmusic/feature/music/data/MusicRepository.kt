package com.lin0721.linmusic.feature.music.data

import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.feature.music.domain.MusicStyle
import com.lin0721.linmusic.feature.music.domain.StyleArtistItem
import com.lin0721.linmusic.feature.music.domain.StyleHead
import com.lin0721.linmusic.feature.music.domain.StylePlaylistItem
import com.lin0721.linmusic.feature.music.domain.StylePreference
import kotlinx.coroutines.flow.Flow

// 「」tab
interface MusicRepository {

    // Genres，（）
    fun getStyleList(): Flow<Result<List<MusicStyle>>>

    // of Genres，Back
    fun getStylePreferences(): Flow<Result<List<StylePreference>>>

    // GenresDetails
    fun getStyleHead(tagId: Long): Flow<Result<StyleHead>>

    // Genres of Popular Playlists
    fun getStylePlaylists(tagId: Long): Flow<Result<List<StylePlaylistItem>>>

    // Genres of Tracks
    fun getStyleSongs(tagId: Long): Flow<Result<List<Track>>>

    // Genres of Top Artists
    fun getStyleArtists(tagId: Long): Flow<Result<List<StyleArtistItem>>>
}
