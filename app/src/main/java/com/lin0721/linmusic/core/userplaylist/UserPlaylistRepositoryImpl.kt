package com.lin0721.linmusic.core.userplaylist

import com.lin0721.linmusic.core.firebase.FirestorePlaylistManager
import com.lin0721.linmusic.core.network.apiFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UserPlaylistRepositoryImpl(
    private val apiService: UserPlaylistApi,
    private val firestorePlaylistManager: FirestorePlaylistManager? = null
) : UserPlaylistRepository {

    override fun getUserPlaylists(uid: Long, limit: Int): Flow<Result<List<UserPlaylist>>> {
        if (firestorePlaylistManager?.isFirebaseUser() == true) {
            return flow {
                val playlists = firestorePlaylistManager.getUserPlaylists(uid)
                emit(Result.success(playlists))
            }
        }
        return apiFlow(
            request = { apiService.getUserPlaylists(UserPlaylistRequest(uid = uid, limit = limit)) },
            isSuccess = { it.isSuccess },
            code = { it.code },
            transform = { it.playlist }
        )
    }
}
