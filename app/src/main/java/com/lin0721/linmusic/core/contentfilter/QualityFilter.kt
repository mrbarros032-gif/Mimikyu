package com.lin0721.linmusic.core.contentfilter

/**
 * The 4-Tier Anti-Spam Filter Pipeline
 * Runs every incoming track, playlist, or album through programmatic checks.
 * If a release fails any of these rules, it is dropped from the homescreen array silently.
 */
object QualityFilter {

    private val spamKeywords = listOf(
        "speed up", "sped up", "spedup", "slowed", "reverb", "nightcore", "lofi remix", 
        "tiktok version", "bass boosted", "1 hour loop", "phonk remix",
        "instrumental karaoke", "parody", "tribute band", "cover version",
        "slowed + reverb", "slowed and reverb", "slowed down", "ringtone", "alarm tone"
    )

    private val trustedDistributors = listOf(
        "Sony Music", "Universal Music", "Warner Music", "Interscope", 
        "Republic Records", "Def Jam", "RCA Records", "Columbia", "Atlantic",
        "Maestro Blink", "T-Series", "YouTube Music"
    )

    fun isHighQualityRelease(
        title: String,
        artistName: String,
        channelSubscriberCount: Int? = null,
        viewCount: Long? = null
    ): Boolean {
        val lowerTitle = title.lowercase()
        val lowerArtist = artistName.lowercase()

        // 1. SPAM WORD BLOCKLIST
        // Drop anything trying to game the algorithm with "Speed up", "Nightcore", or "Reverb"
        if (spamKeywords.any { lowerTitle.contains(it) }) return false

        // 2. THE BOT / NPC ARTIST CHECK
        // If the channel has virtually zero presence, it's usually an automated upload bot
        // (Require at least 1,000 subscribers or verified status for "New Release" shelves)
        if (channelSubscriberCount != null && channelSubscriberCount < 1_000) {
            // Exception for brand new indie artists if they match trusted distributors
            if (!trustedDistributors.any { lowerArtist.contains(it.lowercase()) }) {
                return false
            }
        }

        // 3. TITLE LENGTH & CHARACTER SPAM
        // Spam tracks often use excessive emojis or all-caps clickbait strings like "TOP HIT 2026 🔥🔥🔥"
        if (title.contains("🔥") || title.contains("💎") || (title.length > 5 && title.count { it.isUpperCase() } > title.length * 0.7)) {
            if (!trustedDistributors.any { lowerArtist.contains(it.lowercase()) }) {
                return false
            }
        }

        return true
    }
}
