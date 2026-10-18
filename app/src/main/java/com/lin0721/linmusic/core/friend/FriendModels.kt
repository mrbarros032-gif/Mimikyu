package com.lin0721.linmusic.core.friend

import kotlinx.serialization.Serializable

@Serializable
data class FriendUser(
    val uid: String,
    val numericUid: Long = 0L,
    val username: String,
    val displayName: String = "",
    val avatarUrl: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val latestActivity: FriendActivity? = null
)

@Serializable
data class FollowEntity(
    val id: String = "",
    val type: String = "user", // "user" or "artist"
    val displayName: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val followedAt: Long = System.currentTimeMillis()
)

@Serializable
data class LiveTrackInfo(
    val songId: String = "",
    val title: String = "",
    val artist: String = "",
    val coverUrl: String = "",
    val isPlaying: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class MutualFriendActivity(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val avatarUrl: String = "",
    val currentTrack: LiveTrackInfo? = null,
    val isMutual: Boolean = true
)

@Serializable
data class FriendRequest(
    val senderUid: String,
    val username: String,
    val avatarUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "pending"
)

@Serializable
data class FriendActivity(
    val type: String = "listening", // "playlist", "artist", "album", "listening"
    val title: String = "",
    val subtitle: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val songId: String = "",
    val coverUrl: String = "",
    val isPlaying: Boolean = false
)

@Serializable
data class FriendNotification(
    val id: String = "",
    val type: String = "", // "playlist_created", "artist_followed", "album_added", "friend_request", "friend_accepted"
    val senderUid: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
