package com.lin0721.linmusic.feature.home.domain

// （UI ， and  DTO ）
data class ToplistInfo(
    val id: Long,
    val name: String,
    val coverUrl: String,
    val updateDesc: String,
    // ："Title - Artist"
    val topSongs: List<String>
)
