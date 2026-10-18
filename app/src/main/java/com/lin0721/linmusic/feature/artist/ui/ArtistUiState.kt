package com.lin0721.linmusic.feature.artist.ui

import com.lin0721.linmusic.core.model.ArtistAlbum
import com.lin0721.linmusic.core.model.ArtistDetailInfo
import com.lin0721.linmusic.core.model.ArtistInfo
import com.lin0721.linmusic.core.model.ArtistMv
import com.lin0721.linmusic.core.model.Track

// ArtistsDetails UI
sealed interface ArtistUiState {
    data object Loading : ArtistUiState

    // ，ArtistsDetails and  of Songs/Albums/Artists， of
    data class Success(
        val artist: ArtistDetailInfo,
        val isFollowed: Boolean,
        val fansCount: Long,
        val playCount: Long = 0L,
        val mimikyuMonthlyListeners: Long = 0L,
        val topSongs: List<Track>,
        val albums: List<ArtistAlbum>,
        val albumsHasMore: Boolean = false,
        val albumsLoadingMore: Boolean = false,
        val similarArtists: List<ArtistInfo>,
        val mvs: List<ArtistMv> = emptyList(),
        val mvsHasMore: Boolean = false,
        val mvsLoadingMore: Boolean = false,
        // 「AllSongs」 Tab ，
        val allSongs: List<Track> = emptyList(),
        val allSongsHasMore: Boolean = false,
        val allSongsLoadingMore: Boolean = false,
        val allSongsLoaded: Boolean = false
    ) : ArtistUiState

    data class Error(val message: String) : ArtistUiState
}
