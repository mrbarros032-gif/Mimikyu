package com.lin0721.linmusic.feature.newworks.data

import com.lin0721.linmusic.feature.newworks.domain.NewWorksMv
import com.lin0721.linmusic.feature.newworks.domain.NewWorksReleasePage
import kotlinx.coroutines.flow.Flow

// FollowArtists（feature/newworks）， tab「」 of
interface NewWorksRepository {

    // New MVs，，、
    fun getMvs(): Flow<Result<List<NewWorksMv>>>

    // New Releases（Tracks/Albums），before ：Current of  publishTime
    fun getReleases(before: Long, firstRequest: Boolean): Flow<Result<NewWorksReleasePage>>
}
