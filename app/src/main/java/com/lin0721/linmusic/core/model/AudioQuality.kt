package com.lin0721.linmusic.core.model

// ， settings/player  of  UI 。
fun getQualityDisplayName(quality: String): String {
    return when (quality) {
        "studio_master" -> "Studio Master (Harman)"
        "bass_boost" -> "Desi Hip-Hop / Bass Boost"
        "vocal_acoustic" -> "Late Night Acoustic / Vocal"
        "flat" -> "Flat / Raw Studio"
        "standard" -> "Studio Master (Harman)"
        "higher" -> "Higher"
        "exhigh" -> "Studio Master (Harman)"
        "lossless" -> "Studio Master (Harman)"
        "hires" -> "Hi-Res Lossless"
        "jyeffect" -> "HD Surround"
        "sky" -> "Immersive Surround"
        "jymaster" -> "Studio Master (Harman)"
        else -> quality
    }
}
