package com.lin0721.linmusic.core.songlike

import com.lin0721.linmusic.core.auth.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

// Current of Songs ID， artist/player/playlist
class LoadLikedSongIdsUseCase(
    private val userPreferences: UserPreferences,
    private val songLikeRepository: SongLikeRepository
) {

    // Back null，
    suspend operator fun invoke(): Set<Long>? {
        val profile = userPreferences.userProfile.first() ?: return null
        return songLikeRepository.getLikedSongIds(profile.uid)
            .firstOrNull()
            ?.getOrNull()
            ?.toSet()
    }
}
