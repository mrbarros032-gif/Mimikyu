package com.lin0721.linmusic.core.auth

import kotlinx.coroutines.flow.firstOrNull

// of ， home/library/artist/playlist
class SyncProfileAfterLoginUseCase(
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository
) {

    // Save Cookie ，Back of ；Back null，
    suspend operator fun invoke(cookies: String): UserProfile? {
        userPreferences.saveCookies(cookies)
        if (cookies.startsWith("google_auth_token=")) {
            return userPreferences.userProfile.firstOrNull()
        }
        val response = authRepository.getAccountInfo().firstOrNull()?.getOrNull() ?: return null
        val remoteProfile = response.profile ?: return null
        val profile = UserProfile(
            uid = remoteProfile.userId,
            nickname = remoteProfile.nickname,
            avatarUrl = remoteProfile.avatarUrl
        )
        userPreferences.saveUserProfile(profile)
        return profile
    }
}
