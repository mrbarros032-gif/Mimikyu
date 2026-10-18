package com.lin0721.linmusic.feature.artist.domain

import com.lin0721.linmusic.core.model.ArtistAlbum
import com.lin0721.linmusic.core.model.ArtistMv
import com.lin0721.linmusic.core.model.Track

// ArtistsAlbums
data class ArtistAlbumPage(
    val albums: List<ArtistAlbum>,
    val hasMore: Boolean
)

// Artists MV
data class ArtistMvPage(
    val mvs: List<ArtistMv>,
    val hasMore: Boolean
)

// ArtistsAllSongs
data class ArtistSongsPage(
    val songs: List<Track>,
    val hasMore: Boolean
)

// MV Details（）
data class MvDetail(
    val id: Long,
    val name: String,
    val artistId: Long,
    val artistName: String,
    val cover: String,
    val duration: Long,
    val playCount: Long,
    val subCount: Long,
    val commentCount: Long,
    val likedCount: Long,
    val isSubscribed: Boolean,
    val isLiked: Boolean,
    val publishTime: String,
    val briefDesc: String
)
