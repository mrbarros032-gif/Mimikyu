package com.lin0721.linmusic.feature.search.domain

import com.lin0721.linmusic.core.model.PlaylistDetail

// Curated Playlists of ，nextCursor  of  lasttime （ offset）
data class PlaylistCategoryPage(
    val playlists: List<PlaylistDetail>,
    val hasMore: Boolean,
    val nextCursor: Long,
    val total: Int
)
