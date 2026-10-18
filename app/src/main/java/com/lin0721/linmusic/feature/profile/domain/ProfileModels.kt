package com.lin0721.linmusic.feature.profile.domain

// Details（）
data class ProfileUserInfo(
    val uid: Long,
    val nickname: String,
    val avatarUrl: String,
    val signature: String,
    val level: Int,
    val followsCount: Int,
    val followedsCount: Int,
    val playlistCount: Int,
    val isFollowedByMe: Boolean
)

// Playlists
data class ProfilePlaylistInfo(
    val id: Long,
    val name: String,
    val coverImgUrl: String,
    val playCount: Long,
    val trackCount: Int,
    val creatorName: String
)

// Playlists
data class ProfilePlaylistPage(
    val playlists: List<ProfilePlaylistInfo>,
    val hasMore: Boolean
)

// Follow/Followers
data class ProfileFollowUserItem(
    val uid: Long,
    val nickname: String,
    val avatarUrl: String,
    val signature: String,
    val isFollowedByMe: Boolean
)

// Follow/Followers
data class ProfileFollowListPage(
    val users: List<ProfileFollowUserItem>,
    val hasMore: Boolean
)

// Moments
data class ProfileEventInfo(
    val id: Long,
    val showTime: Long,
    val authorUid: Long,
    val authorNickname: String,
    val authorAvatarUrl: String,
    val rawJson: String
)

// Moments
data class ProfileEventPage(
    val events: List<ProfileEventInfo>,
    val hasMore: Boolean,
    val lasttime: Long
)

// Listening Rank
data class ProfileListenRankItem(
    val playCount: Int,
    val songId: Long,
    val songName: String,
    val artistName: String,
    val albumCoverUrl: String
)
