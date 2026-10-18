package com.lin0721.linmusic.core.friend

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.lin0721.linmusic.core.auth.UserPreferences
import com.lin0721.linmusic.core.auth.UserProfile
import com.lin0721.linmusic.core.auth.awaitTask
import com.lin0721.linmusic.core.log.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private const val TAG = "FriendManager"

class UsernameAlreadyTakenException(message: String) : Exception(message)

class FriendManager(
    private val context: Context,
    private val userPreferences: UserPreferences,
    private val notificationHelper: FriendNotificationHelper
) {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _incomingNotifications = MutableSharedFlow<FriendNotification>(extraBufferCapacity = 10)
    val incomingNotifications: SharedFlow<FriendNotification> = _incomingNotifications.asSharedFlow()

    private val _localAddedFriends = kotlinx.coroutines.flow.MutableStateFlow<List<FriendUser>>(emptyList())

    private var notificationsListener: ListenerRegistration? = null
    private var lastSeenNotificationTime = System.currentTimeMillis()

    init {
        scope.launch {
            val isLoggedIn = userPreferences.isLoggedIn.first()
            if (isLoggedIn) {
                ensureFirebaseUser()
                getOrInitUserProfile()
                startListeningForNotifications()
            }
        }
    }

    private suspend fun ensureFirebaseUser(): String {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            return currentUser.uid
        }
        val isLoggedIn = userPreferences.isLoggedIn.first()
        if (!isLoggedIn) {
            return "guest"
        }
        return try {
            val result = auth.signInAnonymously().awaitTask()
            result.user?.uid ?: "anonymous_${System.currentTimeMillis()}"
        } catch (e: Exception) {
            AppLogger.w(TAG, "Anonymous sign-in failed, fallback to local identifier", e)
            val profile = userPreferences.userProfile.firstOrNull()
            "user_${profile?.uid ?: System.currentTimeMillis()}"
        }
    }

    suspend fun getEffectiveUid(): String {
        return auth.currentUser?.uid ?: ensureFirebaseUser()
    }

    suspend fun getOrInitUserProfile(): UserProfile {
        val saved = userPreferences.userProfile.firstOrNull()
        if (saved != null && saved.nickname.isNotBlank() && !saved.nickname.startsWith("User Account")) {
            val effectiveUsername = if (saved.username.isNotBlank()) saved.username else saved.nickname.lowercase().replace(" ", "_")
            val withUsername = if (saved.username.isBlank()) saved.copy(username = effectiveUsername) else saved
            syncUserToFirestore(withUsername)
            return withUsername
        }

        val isLoggedIn = userPreferences.isLoggedIn.first()
        if (!isLoggedIn) {
            // Guest mode: Return clean unauthenticated guest profile WITHOUT persisting fake username
            return UserProfile(
                uid = 0L,
                nickname = "Guest",
                avatarUrl = "",
                username = ""
            )
        }

        // Only for authenticated users needing a handle
        val randomNum = (1000..9999).random()
        val prefixes = listOf("Mimikyu", "VibeHunter", "EchoSonic", "SoundWave", "RhythmFlow", "AstroBeat", "NovaTune")
        val generatedName = "${prefixes.random()}_$randomNum"
        val generatedUid = System.currentTimeMillis() % 1_000_000_000L + randomNum
        val generatedUsername = generatedName.lowercase()

        val newProfile = UserProfile(
            uid = generatedUid,
            nickname = generatedName,
            avatarUrl = saved?.avatarUrl.orEmpty(),
            username = generatedUsername
        )
        userPreferences.saveUserProfile(newProfile, markAuthenticated = true)
        syncUserToFirestore(newProfile)
        return newProfile
    }

    suspend fun syncUserToFirestore(profile: UserProfile) {
        try {
            val fbUid = getEffectiveUid()
            val usernameVal = if (profile.username.isNotBlank()) profile.username else profile.nickname.lowercase().replace(" ", "_")
            val userData = hashMapOf(
                "uid" to fbUid,
                "numericUid" to profile.uid,
                "displayName" to profile.nickname,
                "nickname" to profile.nickname,
                "username" to usernameVal,
                "usernameLower" to usernameVal.lowercase(),
                "avatarUrl" to profile.avatarUrl,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(fbUid)
                .set(userData, SetOptions.merge())
                .awaitTask()

            // Index username
            firestore.collection("usernames").document(usernameVal.lowercase())
                .set(mapOf("uid" to fbUid, "username" to usernameVal), SetOptions.merge())
                .awaitTask()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to sync user to Firestore", e)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // REQUIREMENT 2: UPDATE USER PROFILE (Name, Username, Avatar)
    // ─────────────────────────────────────────────────────────────
    suspend fun updateUserProfile(
        newDisplayName: String,
        newUsername: String,
        newAvatarUri: Uri?
    ): Result<UserProfile> {
        val trimmedName = newDisplayName.trim()
        val trimmedUsername = newUsername.trim()
        val lowerUsername = trimmedUsername.lowercase()

        if (trimmedName.isEmpty() || trimmedName.length > 40) {
            return Result.failure(IllegalArgumentException("Display Name must be between 1 and 40 characters"))
        }

        val usernamePattern = Regex("^[a-zA-Z0-9_.]+$")
        if (trimmedUsername.length < 3 || trimmedUsername.length > 25 || !trimmedUsername.matches(usernamePattern)) {
            return Result.failure(IllegalArgumentException("Username must be 3-25 characters using only letters, numbers, underscores, and periods"))
        }

        return try {
            val fbUid = getEffectiveUid()
            val currentProfile = getOrInitUserProfile()
            var uploadedAvatarUrl: String? = null

            // 1. Upload compressed avatar if provided
            if (newAvatarUri != null) {
                val compressedBytes = ImageCompressor.compressImage(context, newAvatarUri, maxDimension = 512, quality = 80)
                if (compressedBytes != null && compressedBytes.isNotEmpty()) {
                    val storageRef = storage.reference.child("profile_pictures/$fbUid/avatar.jpg")
                    storageRef.putBytes(compressedBytes).awaitTask()
                    uploadedAvatarUrl = storageRef.downloadUrl.awaitTask().toString()
                }
            }

            val finalAvatarUrl = uploadedAvatarUrl ?: currentProfile.avatarUrl

            // 2. Firestore Transaction: atomic username claim and profile update
            firestore.runTransaction { tx ->
                val userDocRef = firestore.collection("users").document(fbUid)
                val userSnapshot = tx.get(userDocRef)
                val oldUsernameLower = userSnapshot.getString("usernameLower")
                    ?: currentProfile.username.lowercase().ifBlank { currentProfile.nickname.lowercase() }

                // Check and update username if changed
                if (oldUsernameLower != lowerUsername) {
                    val newUsernameDocRef = firestore.collection("usernames").document(lowerUsername)
                    val newUsernameSnapshot = tx.get(newUsernameDocRef)

                    if (newUsernameSnapshot.exists()) {
                        val claimedUid = newUsernameSnapshot.getString("uid")
                        if (claimedUid != null && claimedUid != fbUid) {
                            throw UsernameAlreadyTakenException("Username '@$trimmedUsername' is already taken")
                        }
                    }

                    // Release old username
                    if (oldUsernameLower.isNotBlank() && oldUsernameLower != lowerUsername) {
                        val oldUsernameDocRef = firestore.collection("usernames").document(oldUsernameLower)
                        tx.delete(oldUsernameDocRef)
                    }

                    // Claim new username
                    tx.set(newUsernameDocRef, mapOf(
                        "uid" to fbUid,
                        "username" to trimmedUsername,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ))
                }

                val updatePayload = hashMapOf<String, Any>(
                    "uid" to fbUid,
                    "displayName" to trimmedName,
                    "nickname" to trimmedName,
                    "username" to trimmedUsername,
                    "usernameLower" to lowerUsername,
                    "avatarUrl" to finalAvatarUrl,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                tx.set(userDocRef, updatePayload, SetOptions.merge())
            }.awaitTask()

            // 3. Sync to FirebaseAuth Profile
            try {
                val changeRequest = userProfileChangeRequest {
                    displayName = trimmedName
                    if (finalAvatarUrl.isNotBlank()) {
                        photoUri = Uri.parse(finalAvatarUrl)
                    }
                }
                auth.currentUser?.updateProfile(changeRequest)?.awaitTask()
            } catch (e: Exception) {
                AppLogger.w(TAG, "FirebaseAuth updateProfile non-critical failure", e)
            }

            // 4. Update local DataStore immediately
            val updatedProfile = currentProfile.copy(
                nickname = trimmedName,
                username = trimmedUsername,
                avatarUrl = finalAvatarUrl
            )
            userPreferences.saveUserProfile(updatedProfile)

            Result.success(updatedProfile)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to update profile", e)
            Result.failure(e)
        }
    }

    fun isUsernameAvailable(username: String): Flow<Boolean> = callbackFlow {
        val trimmed = username.trim().lowercase()
        if (trimmed.length < 3) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val myUid = auth.currentUser?.uid
        val listener = firestore.collection("usernames").document(trimmed)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(true)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    trySend(true)
                } else {
                    val uid = snapshot.getString("uid")
                    trySend(uid == null || uid == myUid)
                }
            }
        awaitClose { listener.remove() }
    }

    // ─────────────────────────────────────────────────────────────
    // REQUIREMENT 2: SOCIAL GRAPH (Follow / Unfollow / Counters)
    // ─────────────────────────────────────────────────────────────
    suspend fun followUser(
        targetUid: String,
        targetDisplayName: String,
        targetUsername: String,
        targetAvatarUrl: String
    ): Result<Unit> {
        return try {
            val myUid = getEffectiveUid()
            val myProfile = getOrInitUserProfile()
            val myUsername = myProfile.username.ifBlank { myProfile.nickname.lowercase() }

            val batch = firestore.batch()

            // 1. Add to my following
            val followingRef = firestore.collection("users").document(myUid)
                .collection("following").document(targetUid)
            batch.set(followingRef, mapOf(
                "id" to targetUid,
                "type" to "user",
                "displayName" to targetDisplayName,
                "username" to targetUsername,
                "avatarUrl" to targetAvatarUrl,
                "followedAt" to FieldValue.serverTimestamp()
            ))

            // 2. Add to target's followers
            val followerRef = firestore.collection("users").document(targetUid)
                .collection("followers").document(myUid)
            batch.set(followerRef, mapOf(
                "uid" to myUid,
                "displayName" to myProfile.nickname,
                "username" to myUsername,
                "avatarUrl" to myProfile.avatarUrl,
                "followedAt" to FieldValue.serverTimestamp()
            ))

            batch.commit().awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error following user: $targetUid", e)
            Result.failure(e)
        }
    }

    suspend fun followArtist(
        artistId: String,
        artistName: String,
        artistImageUrl: String
    ): Result<Unit> {
        return try {
            val myUid = getEffectiveUid()
            val followingRef = firestore.collection("users").document(myUid)
                .collection("following").document(artistId)

            followingRef.set(mapOf(
                "id" to artistId,
                "type" to "artist",
                "displayName" to artistName,
                "avatarUrl" to artistImageUrl,
                "followedAt" to FieldValue.serverTimestamp()
            )).awaitTask()

            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error following artist: $artistId", e)
            Result.failure(e)
        }
    }

    suspend fun unfollowTarget(targetId: String, isArtist: Boolean): Result<Unit> {
        return try {
            val myUid = getEffectiveUid()
            val batch = firestore.batch()

            val followingRef = firestore.collection("users").document(myUid)
                .collection("following").document(targetId)
            batch.delete(followingRef)

            if (!isArtist) {
                val followerRef = firestore.collection("users").document(targetId)
                    .collection("followers").document(myUid)
                batch.delete(followerRef)
            }

            batch.commit().awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error unfollowing target: $targetId", e)
            Result.failure(e)
        }
    }

    fun isFollowing(targetId: String): Flow<Boolean> = callbackFlow {
        val myUid = auth.currentUser?.uid
        if (myUid == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(myUid)
            .collection("following").document(targetId)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot != null && snapshot.exists())
            }
        awaitClose { listener.remove() }
    }

    fun observeFollowersCount(targetUid: String? = null): Flow<Int> = callbackFlow {
        val uid = targetUid ?: auth.currentUser?.uid
        if (uid == null) {
            trySend(0)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(uid)
            .collection("followers")
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    fun observeFollowingCount(targetUid: String? = null): Flow<Int> = callbackFlow {
        val uid = targetUid ?: auth.currentUser?.uid
        if (uid == null) {
            trySend(0)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(uid)
            .collection("following")
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    fun getFollowingList(targetUid: String? = null): Flow<List<FollowEntity>> = callbackFlow {
        val uid = targetUid ?: auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(uid)
            .collection("following")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    FollowEntity(
                        id = doc.getString("id") ?: doc.id,
                        type = doc.getString("type") ?: "user",
                        displayName = doc.getString("displayName") ?: doc.getString("name").orEmpty(),
                        username = doc.getString("username").orEmpty(),
                        avatarUrl = doc.getString("avatarUrl") ?: doc.getString("imageUrl").orEmpty(),
                        followedAt = doc.getTimestamp("followedAt")?.toDate()?.time ?: System.currentTimeMillis()
                    )
                }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getFollowersList(targetUid: String? = null): Flow<List<FollowEntity>> = callbackFlow {
        val uid = targetUid ?: auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(uid)
            .collection("followers")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    FollowEntity(
                        id = doc.getString("uid") ?: doc.id,
                        type = "user",
                        displayName = doc.getString("displayName") ?: doc.getString("username").orEmpty(),
                        username = doc.getString("username").orEmpty(),
                        avatarUrl = doc.getString("avatarUrl").orEmpty(),
                        followedAt = doc.getTimestamp("followedAt")?.toDate()?.time ?: System.currentTimeMillis()
                    )
                }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ─────────────────────────────────────────────────────────────
    // REQUIREMENT 2: MUTUAL FRIENDS & LIVE TRACK STREAM
    // ─────────────────────────────────────────────────────────────
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMutualFriendsActivityFlow(): Flow<List<MutualFriendActivity>> {
        val myUid = auth.currentUser?.uid ?: return flowOf(emptyList())

        // 1. Observe Following (type == "user")
        val followingUsersFlow: Flow<Set<String>> = callbackFlow {
            val listener = firestore.collection("users").document(myUid)
                .collection("following")
                .whereEqualTo("type", "user")
                .addSnapshotListener { snapshot, _ ->
                    val ids = snapshot?.documents?.map { it.id }?.toSet().orEmpty()
                    trySend(ids)
                }
            awaitClose { listener.remove() }
        }

        // 2. Observe Followers
        val followersFlow: Flow<Set<String>> = callbackFlow {
            val listener = firestore.collection("users").document(myUid)
                .collection("followers")
                .addSnapshotListener { snapshot, _ ->
                    val ids = snapshot?.documents?.map { it.id }?.toSet().orEmpty()
                    trySend(ids)
                }
            awaitClose { listener.remove() }
        }

        // 3. Compute mutual UIDs and observe each mutual friend's document
        return combine(followingUsersFlow, followersFlow) { following, followers ->
            following.intersect(followers)
        }.flatMapLatest { mutualUids ->
            if (mutualUids.isEmpty()) {
                flowOf(emptyList())
            } else {
                callbackFlow {
                    // Observe each mutual friend's document
                    val registrations = mutableListOf<ListenerRegistration>()
                    val activitiesMap = mutableMapOf<String, MutualFriendActivity>()

                    for (uid in mutualUids) {
                        val reg = firestore.collection("users").document(uid)
                            .addSnapshotListener { doc, _ ->
                                if (doc != null && doc.exists()) {
                                    val name = doc.getString("displayName") ?: doc.getString("username").orEmpty()
                                    val username = doc.getString("username").orEmpty()
                                    val avatarUrl = doc.getString("avatarUrl").orEmpty()

                                    @Suppress("UNCHECKED_CAST")
                                    val trackMap = doc.get("currentTrack") as? Map<String, Any>
                                    val liveTrack = if (trackMap != null) {
                                        LiveTrackInfo(
                                            songId = trackMap["songId"]?.toString().orEmpty(),
                                            title = trackMap["title"]?.toString().orEmpty(),
                                            artist = trackMap["artist"]?.toString().orEmpty(),
                                            coverUrl = trackMap["coverUrl"]?.toString().orEmpty(),
                                            isPlaying = trackMap["isPlaying"] as? Boolean ?: false,
                                            updatedAt = (trackMap["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                                        )
                                    } else null

                                    activitiesMap[uid] = MutualFriendActivity(
                                        uid = uid,
                                        displayName = name,
                                        username = username,
                                        avatarUrl = avatarUrl,
                                        currentTrack = liveTrack,
                                        isMutual = true
                                    )
                                    trySend(activitiesMap.values.toList())
                                }
                            }
                        registrations.add(reg)
                    }

                    awaitClose {
                        registrations.forEach { it.remove() }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // REQUIREMENT 2: NOW PLAYING LIVE SYNC
    // ─────────────────────────────────────────────────────────────
    suspend fun updateCurrentPlayingTrack(
        songId: String,
        title: String,
        artist: String,
        coverUrl: String,
        isPlaying: Boolean
    ) {
        val myUid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(myUid).set(
                mapOf(
                    "currentTrack" to mapOf(
                        "songId" to songId,
                        "title" to title,
                        "artist" to artist,
                        "coverUrl" to coverUrl,
                        "isPlaying" to isPlaying,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ),
                SetOptions.merge()
            ).awaitTask()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Error updating current track to Firestore", e)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SEARCH & PROFILE DISCOVERY
    // ─────────────────────────────────────────────────────────────
    suspend fun searchUsers(query: String): List<FriendUser> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()

        return try {
            val myUid = getEffectiveUid()
            val snapshot = firestore.collection("users")
                .orderBy("usernameLower")
                .startAt(trimmed)
                .endAt(trimmed + "\uf8ff")
                .limit(20)
                .get()
                .awaitTask()

            snapshot.documents.mapNotNull { doc ->
                val uid = doc.getString("uid") ?: doc.id
                if (uid == myUid) return@mapNotNull null

                val username = doc.getString("username").orEmpty()
                val displayName = doc.getString("displayName") ?: username
                val avatarUrl = doc.getString("avatarUrl").orEmpty()
                val numericUid = doc.getLong("numericUid") ?: 0L

                FriendUser(
                    uid = uid,
                    numericUid = numericUid,
                    username = username,
                    displayName = displayName,
                    avatarUrl = avatarUrl
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error searching users", e)
            emptyList()
        }
    }

    suspend fun getUserProfileByUid(uid: String): FriendUser? {
        return try {
            val doc = firestore.collection("users").document(uid).get().awaitTask()
            if (doc.exists()) {
                val username = doc.getString("username").orEmpty()
                val displayName = doc.getString("displayName") ?: username
                val avatarUrl = doc.getString("avatarUrl").orEmpty()
                val numericUid = doc.getLong("numericUid") ?: 0L
                FriendUser(
                    uid = uid,
                    numericUid = numericUid,
                    username = username,
                    displayName = displayName,
                    avatarUrl = avatarUrl
                )
            } else null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error fetching user profile by uid: $uid", e)
            null
        }
    }

    // ─────────────────────────────────────────────────────────────
    // LEGACY & NOTIFICATIONS SUPPORT
    // ─────────────────────────────────────────────────────────────
    fun observeFriends(): Flow<List<FriendUser>> = callbackFlow {
        val myUid = auth.currentUser?.uid
        if (myUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(myUid)
            .collection("friends")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val uid = doc.getString("uid") ?: doc.id
                    val username = doc.getString("username").orEmpty()
                    val displayName = doc.getString("displayName") ?: username
                    val avatarUrl = doc.getString("avatarUrl").orEmpty()
                    FriendUser(
                        uid = uid,
                        username = username,
                        displayName = displayName,
                        avatarUrl = avatarUrl
                    )
                }.orEmpty()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun observeFriendRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val myUid = auth.currentUser?.uid
        if (myUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(myUid)
            .collection("friend_requests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val senderUid = doc.getString("senderUid") ?: doc.id
                    val username = doc.getString("username").orEmpty()
                    val avatarUrl = doc.getString("avatarUrl").orEmpty()
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    FriendRequest(senderUid, username, avatarUrl, timestamp)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun startListeningForNotifications() {
        notificationsListener?.remove()
        val myUid = auth.currentUser?.uid ?: return

        notificationsListener = firestore.collection("users").document(myUid)
            .collection("notifications")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                for (doc in snapshot.documents) {
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    if (timestamp > lastSeenNotificationTime) {
                        lastSeenNotificationTime = timestamp
                        val notif = FriendNotification(
                            id = doc.id,
                            type = doc.getString("type").orEmpty(),
                            senderUid = doc.getString("senderUid").orEmpty(),
                            senderName = doc.getString("senderName").orEmpty(),
                            senderAvatar = doc.getString("senderAvatar").orEmpty(),
                            title = doc.getString("title").orEmpty(),
                            message = doc.getString("message").orEmpty(),
                            timestamp = timestamp
                        )
                        _incomingNotifications.tryEmit(notif)
                        notificationHelper.showFriendNotification(notif)
                    }
                }
            }
    }

    suspend fun broadcastActivity(type: String, targetName: String, extra: String = ""): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val act = hashMapOf(
                "type" to type,
                "targetName" to targetName,
                "extra" to extra,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("users").document(uid).collection("activity").add(act).awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendFriendRequest(targetUid: String, targetUsername: String): Result<Unit> {
        return followUser(targetUid = targetUid, targetDisplayName = targetUsername, targetUsername = targetUsername, targetAvatarUrl = "")
    }

    suspend fun acceptFriendRequest(senderUid: String, senderUsername: String, senderAvatarUrl: String): Result<Unit> {
        return followUser(targetUid = senderUid, targetDisplayName = senderUsername, targetUsername = senderUsername, targetAvatarUrl = senderAvatarUrl)
    }

    suspend fun declineFriendRequest(senderUid: String): Result<Unit> {
        return unfollowTarget(senderUid, isArtist = false)
    }
}
