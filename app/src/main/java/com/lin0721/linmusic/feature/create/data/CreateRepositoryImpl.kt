package com.lin0721.linmusic.feature.create.data

import com.lin0721.linmusic.core.firebase.FirestorePlaylistManager
import com.lin0721.linmusic.core.model.PlaylistDetail
import com.lin0721.linmusic.core.network.apiFlow
import com.lin0721.linmusic.core.userplaylist.LocalCustomPlaylistStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CreateRepositoryImpl(
    private val apiService: CreateApi,
    private val firestorePlaylistManager: FirestorePlaylistManager? = null,
    private val localCustomPlaylistStore: LocalCustomPlaylistStore? = null
) : CreateRepository {

    override fun createPlaylist(name: String, privacy: Int): Flow<Result<PlaylistDetail>> {
        if (firestorePlaylistManager?.isFirebaseUser() == true) {
            return flow {
                try {
                    val detail = firestorePlaylistManager.createPlaylist(name, privacy)
                    emit(Result.success(detail))
                } catch (e: Exception) {
                    emit(Result.failure(e))
                }
            }
        }
        if (localCustomPlaylistStore != null) {
            return flow {
                try {
                    val detail = localCustomPlaylistStore.createPlaylist(name, emptyList())
                    emit(Result.success(detail))
                } catch (e: Exception) {
                    emit(Result.failure(e))
                }
            }
        }
        return apiFlow(
            request = { apiService.createPlaylist(PlaylistCreateRequest(name = name, privacy = privacy)) },
            isSuccess = { it.isSuccess && it.playlist != null },
            code = { it.code },
            transform = { it.playlist!! }
        )
    }
}
