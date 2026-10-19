package com.lin0721.linmusic.core.update.domain

// of
data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val isPrerelease: Boolean,
    val changelog: String,
    val apkDownloadUrl: String,
    val releasePageUrl: String,
    val title: String? = null
)
