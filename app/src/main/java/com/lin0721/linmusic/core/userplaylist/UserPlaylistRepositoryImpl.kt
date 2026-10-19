package com.lin0721.linmusic.core.userplaylist

import com.lin0721.linmusic.core.firebase.FirestorePlaylistManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UserPlaylistRepositoryImpl(
    private val apiService: UserPlaylistApi,
    private val firestorePlaylistManager: FirestorePlaylistManager? = null,
    private val localPlaylistStore: LocalCustomPlaylistStore? = null
) : UserPlaylistRepository {

    override fun getUserPlaylists(uid: Long, limit: Int): Flow<Result<List<UserPlaylist>>> = flow {
        val localPlaylists = localPlaylistStore?.getPlaylists().orEmpty()
        val remotePlaylists = when {
            firestorePlaylistManager?.isFirebaseUser() == true -> {
                firestorePlaylistManager.getUserPlaylists(uid)
            }
            uid > 0L -> {
                runCatching {
                    apiService.getUserPlaylists(UserPlaylistRequest(uid = uid, limit = limit)).playlist
                }.getOrDefault(emptyList())
            }
            else -> emptyList()
        }
        val combined = (localPlaylists + remotePlaylists).distinctBy { it.id }
        emit(Result.success(combined))
    }
}
