package com.lin0721.linmusic.feature.listendata.domain

// /Listening Report，、， UI
data class ListenReport(
    // ，
    val playMinutes: Int,
    // of ，
    val beyondPercentText: String = "",
    val achievementTitle: String = "",
    val achievementSubtitle: String = "",
    val listenDays: Int = 0,
    // of Songs，
    val wallpaperUrls: List<String> = emptyList(),
    val songCount: Int = 0,
    val highlights: List<Highlight> = emptyList(),
    val dailyDurations: List<DayDuration> = emptyList(),
    val timePeriods: List<TimePeriod> = emptyList(),
    val topArtists: List<TopArtist> = emptyList(),
    val genreName: String = "",
    val ageLabel: String = "",
    val languageName: String = "",
    val vipItems: List<VipItem> = emptyList(),
    val podcastMinutes: Int = 0,
    val podcastEpisodes: Int = 0,
    val friendsListening: List<FriendListening> = emptyList(),
    val friendKeywords: List<String> = emptyList()
)

// of ，label  and  valueText （「」「12」）
data class Highlight(
    val songId: Long,
    val songName: String,
    val coverUrl: String,
    val label: String,
    val valueText: String
)

data class DayDuration(
    // 。，，
    val label: String,
    // of ， of
    val fullLabel: String,
    val minutes: Int
)

data class TimePeriod(
    // （morning / deep_night ），UI
    val key: String,
    val label: String,
    val minutes: Int
)

data class TopArtist(
    val id: Long,
    val name: String,
    val coverUrl: String,
    // 「26」，
    val countText: String
)

data class VipItem(
    val field: String,
    val mainText: String,
    val subText: String
)

data class FriendListening(
    val userId: Long,
    val username: String,
    val avatarUrl: String,
    val songName: String,
    val playCount: Int
)

// /Top Played
data class SongRank(
    val totalCount: Int,
    val songs: List<RankSong>
)

data class RankSong(
    val id: Long,
    val name: String,
    val artistName: String,
    val coverUrl: String,
    val playCount: Int
)

// Today's Plays
data class TodaySong(
    val id: Long,
    val name: String,
    val artistName: String,
    val coverUrl: String
)

// Yearly History
data class YearStat(
    val year: Int,
    val playCount: Int,
    val durationSeconds: Long,
    val label: String = year.toString()
)
