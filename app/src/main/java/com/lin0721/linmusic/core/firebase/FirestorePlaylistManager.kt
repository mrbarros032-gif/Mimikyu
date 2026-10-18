package com.lin0721.linmusic.core.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.lin0721.linmusic.core.auth.awaitTask
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.model.PlaylistCreator
import com.lin0721.linmusic.core.model.PlaylistDetail
import com.lin0721.linmusic.core.model.PlaylistTrackId
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.userplaylist.UserPlaylist
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "FirestorePlaylistMgr"

class FirestorePlaylistManager {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val json = Json { ignoreUnknownKeys = true }

    fun isFirebaseUser(): Boolean = auth.currentUser != null

    private fun getCurrentUid(): String? = auth.currentUser?.uid

    suspend fun getUserPlaylists(uid: Long): List<UserPlaylist> {
        val fbUid = getCurrentUid() ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users")
                .document(fbUid)
                .collection("playlists")
                .get()
                .awaitTask()

            snapshot.documents.mapNotNull { doc ->
                val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                val name = doc.getString("name").orEmpty()
                val coverImgUrl = doc.getString("coverImgUrl").orEmpty()
                val playCount = doc.getLong("playCount") ?: 0L
                val trackCount = doc.getLong("trackCount")?.toInt() ?: 0
                val updateTime = doc.getLong("updateTime") ?: System.currentTimeMillis()
                val creatorName = doc.getString("creatorNickname") ?: auth.currentUser?.displayName.orEmpty()
                val creatorAvatar = doc.getString("creatorAvatar") ?: auth.currentUser?.photoUrl?.toString().orEmpty()

                UserPlaylist(
                    id = id,
                    name = name,
                    coverImgUrl = coverImgUrl,
                    playCount = playCount,
                    trackCount = trackCount,
                    userId = uid,
                    creator = PlaylistCreator(userId = uid, nickname = creatorName, avatarUrl = creatorAvatar),
                    updateTime = updateTime
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error fetching user playlists from Firestore", e)
            emptyList()
        }
    }

    suspend fun getPlaylistDetail(playlistId: Long): PlaylistDetail? {
        val fbUid = getCurrentUid() ?: return null
        return try {
            val doc = firestore.collection("users")
                .document(fbUid)
                .collection("playlists")
                .document(playlistId.toString())
                .get()
                .awaitTask()

            if (!doc.exists()) return null

            val name = doc.getString("name").orEmpty()
            val coverImgUrl = doc.getString("coverImgUrl").orEmpty()
            val description = doc.getString("description")
            val playCount = doc.getLong("playCount") ?: 0L
            val trackCount = doc.getLong("trackCount")?.toInt() ?: 0
            val tracksJson = doc.getString("tracksJson")
            val tracks = if (!tracksJson.isNullOrEmpty()) {
                runCatching { json.decodeFromString<List<Track>>(tracksJson) }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
            val trackIds = tracks.map { PlaylistTrackId(it.id) }
            val creatorName = doc.getString("creatorNickname") ?: auth.currentUser?.displayName.orEmpty()
            val creatorAvatar = doc.getString("creatorAvatar") ?: auth.currentUser?.photoUrl?.toString().orEmpty()

            PlaylistDetail(
                id = playlistId,
                name = name,
                coverImgUrl = coverImgUrl,
                description = description,
                playCount = playCount,
                subscribed = false,
                creator = PlaylistCreator(nickname = creatorName, avatarUrl = creatorAvatar),
                tracks = tracks,
                trackCount = trackCount,
                trackIds = trackIds
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error getting playlist detail from Firestore", e)
            null
        }
    }

    suspend fun createPlaylist(name: String, privacy: Int): PlaylistDetail {
        val fbUid = getCurrentUid() ?: throw IllegalStateException("User not authenticated in Firebase")
        val playlistId = (System.currentTimeMillis() and 0x7FFFFFFFFFFFFFFFL)
        val user = auth.currentUser
        val creatorName = user?.displayName ?: "October User"
        val creatorAvatar = user?.photoUrl?.toString().orEmpty()

        val data = hashMapOf(
            "id" to playlistId,
            "name" to name.trim(),
            "coverImgUrl" to "",
            "description" to "",
            "privacy" to privacy,
            "playCount" to 0L,
            "trackCount" to 0,
            "tracksJson" to "[]",
            "creatorNickname" to creatorName,
            "creatorAvatar" to creatorAvatar,
            "updateTime" to System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(fbUid)
            .collection("playlists")
            .document(playlistId.toString())
            .set(data)
            .awaitTask()

        AppLogger.i(TAG, "Created playlist $name ($playlistId) in Firestore")

        runCatching {
            org.koin.core.context.GlobalContext.get().get<com.lin0721.linmusic.core.friend.FriendManager>()
                .broadcastActivity("playlist", name.trim(), "Playlist")
        }

        return PlaylistDetail(
            id = playlistId,
            name = name.trim(),
            coverImgUrl = "",
            creator = PlaylistCreator(nickname = creatorName, avatarUrl = creatorAvatar),
            trackCount = 0,
            tracks = emptyList(),
            trackIds = emptyList()
        )
    }

    suspend fun createPlaylistWithTracks(
        name: String,
        tracks: List<Track>,
        description: String = "Imported playlist"
    ): PlaylistDetail {
        val fbUid = getCurrentUid()
        val playlistId = (System.currentTimeMillis() and 0x7FFFFFFFFFFFFFFFL)
        val user = auth.currentUser
        val creatorName = user?.displayName ?: "October User"
        val creatorAvatar = user?.photoUrl?.toString().orEmpty()
        val coverUrl = tracks.firstOrNull()?.al?.picUrl.orEmpty()
        val tracksJson = json.encodeToString(tracks)

        if (fbUid != null) {
            val data = hashMapOf(
                "id" to playlistId,
                "name" to name.trim(),
                "coverImgUrl" to coverUrl,
                "description" to description,
                "privacy" to 0,
                "playCount" to 0L,
                "trackCount" to tracks.size,
                "tracksJson" to tracksJson,
                "creatorNickname" to creatorName,
                "creatorAvatar" to creatorAvatar,
                "updateTime" to System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(fbUid)
                .collection("playlists")
                .document(playlistId.toString())
                .set(data)
                .awaitTask()

            AppLogger.i(TAG, "Created playlist $name ($playlistId) with ${tracks.size} tracks in Firestore")
        }

        return PlaylistDetail(
            id = playlistId,
            name = name.trim(),
            coverImgUrl = coverUrl,
            description = description,
            creator = PlaylistCreator(nickname = creatorName, avatarUrl = creatorAvatar),
            trackCount = tracks.size,
            tracks = tracks,
            trackIds = tracks.map { PlaylistTrackId(it.id) }
        )
    }

    suspend fun updatePlaylistTracks(playlistId: Long, tracks: List<Track>): Boolean {
        val fbUid = getCurrentUid() ?: return false
        return try {
            val docRef = firestore.collection("users")
                .document(fbUid)
                .collection("playlists")
                .document(playlistId.toString())

            val coverUrl = tracks.firstOrNull { it.al.picUrl.isNotBlank() }?.al?.picUrl.orEmpty()
            val tracksJson = json.encodeToString(tracks)
            docRef.update(
                mapOf(
                    "coverImgUrl" to coverUrl,
                    "tracksJson" to tracksJson,
                    "trackCount" to tracks.size,
                    "updateTime" to System.currentTimeMillis()
                )
            ).awaitTask()
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "updatePlaylistTracks failed", e)
            false
        }
    }

    suspend fun manipulatePlaylistTracks(op: String, playlistId: Long, trackIds: List<Long>): Boolean {
        val fbUid = getCurrentUid() ?: return false
        return try {
            val docRef = firestore.collection("users")
                .document(fbUid)
                .collection("playlists")
                .document(playlistId.toString())

            val snapshot = docRef.get().awaitTask()
            if (!snapshot.exists()) return false

            val tracksJson = snapshot.getString("tracksJson")
            val currentTracks = if (!tracksJson.isNullOrEmpty()) {
                runCatching { json.decodeFromString<List<Track>>(tracksJson) }.getOrDefault(emptyList()).toMutableList()
            } else {
                mutableListOf()
            }

            if (op == "add") {
                val existingIds = currentTracks.map { it.id }.toSet()
                trackIds.forEach { id ->
                    if (!existingIds.contains(id)) {
                        currentTracks.add(Track(id = id, name = "Track #$id"))
                    }
                }
            } else if (op == "del") {
                val toRemove = trackIds.toSet()
                currentTracks.removeAll { toRemove.contains(it.id) }
            }

            val updatedJson = json.encodeToString(currentTracks.toList())
            docRef.update(
                mapOf(
                    "trackCount" to currentTracks.size,
                    "tracksJson" to updatedJson,
                    "updateTime" to System.currentTimeMillis()
                )
            ).awaitTask()

            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error manipulating playlist tracks in Firestore", e)
            false
        }
    }

    suspend fun renamePlaylist(playlistId: Long, name: String): Boolean {
        val fbUid = getCurrentUid() ?: return false
        return try {
            firestore.collection("users")
                .document(fbUid)
                .collection("playlists")
                .document(playlistId.toString())
                .update("name", name.trim())
                .awaitTask()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deletePlaylist(playlistId: Long): Boolean {
        val fbUid = getCurrentUid() ?: return false
        return try {
            firestore.collection("users")
                .document(fbUid)
                .collection("playlists")
                .document(playlistId.toString())
                .delete()
                .awaitTask()
            true
        } catch (e: Exception) {
            false
        }
    }
}
