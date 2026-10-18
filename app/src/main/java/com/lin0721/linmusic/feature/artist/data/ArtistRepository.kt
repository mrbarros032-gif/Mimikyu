package com.lin0721.linmusic.feature.artist.data

import com.lin0721.linmusic.core.model.ArtistDetailInfo
import com.lin0721.linmusic.core.model.ArtistInfo
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.feature.artist.domain.ArtistAlbumPage
import com.lin0721.linmusic.feature.artist.domain.ArtistMvPage
import com.lin0721.linmusic.feature.artist.domain.ArtistSongsPage
import com.lin0721.linmusic.feature.artist.domain.MvDetail
import kotlinx.coroutines.flow.Flow

// Artists（artist ）
interface ArtistRepository {

    fun getArtistDetail(artistId: Long): Flow<Result<ArtistDetailInfo>>

    fun getArtistAlbums(artistId: Long, limit: Int = 10, offset: Int = 0): Flow<Result<ArtistAlbumPage>>

    // Followers（）
    fun getArtistFansCount(artistId: Long): Flow<Result<Long>>

    // Songs（50）
    fun getArtistTopSongs(artistId: Long): Flow<Result<List<Track>>>

    // Save/FollowArtists
    fun subscribeArtist(artistId: Long, subscribe: Boolean): Flow<Result<Unit>>

    // FollowingArtists
    fun checkArtistFollowed(artistId: Long): Flow<Result<Boolean>>

    fun getSimilarArtists(artistId: Long): Flow<Result<List<ArtistInfo>>>

    // Artists MV （）
    fun getArtistMvs(artistId: Long, limit: Int = 20, offset: Int = 0): Flow<Result<ArtistMvPage>>

    // MV Play
    fun getMvUrl(mvId: Long, resolution: Int = 1080): Flow<Result<String>>

    // ArtistsAllSongs（；， eapi / weapi）
    fun getArtistAllSongs(artistId: Long, offset: Int = 0, limit: Int = 100, order: String = "hot"): Flow<Result<ArtistSongsPage>>

    // MV Details（：Artists/Play/Save//Save）
    fun getMvDetail(mvId: Long): Flow<Result<MvDetail>>

    // Save/CancelSave MV
    fun subscribeMv(mvId: Long, subscribe: Boolean): Flow<Result<Unit>>

    // /Cancel MV
    fun likeMv(mvId: Long, like: Boolean): Flow<Result<Unit>>
}
