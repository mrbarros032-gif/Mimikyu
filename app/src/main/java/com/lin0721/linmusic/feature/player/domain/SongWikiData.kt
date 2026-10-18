package com.lin0721.linmusic.feature.player.domain

// SongsDetails/\u767e\u79d1\u4fe1\u606f\u9886\u57df\u6a21\u578b
data class SongWikiData(
    val style: String = "",
    val album: String = "",
    val language: String = "",
    val publishTime: String = "",
    val bpm: String = "",
    val creators: String = "",
    val creatorRoles: List<SongWikiCreatorRole> = emptyList(),
    val entertainment: String = "",
    val awards: List<String> = emptyList(),
    // \u53ef\u80fd\u591a\u4e8e awards \u5b9e\u9645\u5217\u51fa of \u6761\u6570
    val awardTotal: Int = 0,
    val musicMemory: SongMusicMemory? = null
)

// Producer\u4eba\u5458\u5355\u4e2a\u89d2\u8272（\u5982"Lyrics"）\u53ca\u5176\u5bf9\u5e94 of \u827a\u4eba\u540d\u5355
data class SongWikiCreatorRole(
    val roleName: String,
    val artistNames: List<String>
)

// \u56de\u5fc6\u5750\u6807，\u7f3a\u5931\u9879\u4e3a\u7a7a\u4e32\u6216 0
data class SongMusicMemory(
    val firstListenDate: String,
    val season: String,
    val period: String,
    val playCount: Int,
    val playCountText: String
)
