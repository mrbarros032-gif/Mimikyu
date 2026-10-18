package com.lin0721.linmusic.core.metadata

/**
 * Extensible noise patterns to strip non-musical upload metadata tags
 * from song titles and artist names (e.g. from YouTube / YouTube Music).
 *
 * NOTE: Preserves legitimate musical descriptors like:
 * - "(feat. Artist)"
 * - "(Remix)"
 * - "(Acoustic)"
 * - "(Live)"
 */
private val NOISE_PATTERNS = listOf(
    // Match any bracketed tag containing video/audio upload noise:
    // e.g. (Official Video), (Official Music Video), [Official Video], [Lyrics], (Lyric Video),
    // (Visualizer), [Official Audio], [4K], 【MV】, [Dance Video], etc.
    Regex("""(?i)\s*[\(\[\{【（「『][^()\[\]{}【】（）「」『』]*?\b(?:official\s*(?:music\s*|lyric\s*|visualizer\s*|performance\s*)?video|music\s*video|official\s*audio|official\s*visualizer|short\s*version|extended\s*version|lyrics?\s*(?:video)?|visualizer|audio\s*track|audio\s*oficial|video\s*oficial|clip\s*officiel|full\s*video|official\s*mv|mv|m/v|hd\s*video|4k|8k|ultra\s*hd|performance\s*video|dance\s*video|dance\s*performance)[^()\[\]{}【】（）「」『』]*?[\)\]\}】）」』]"""),
    // Trailing unbracketed noise: e.g. " - Official Video", " - Short Version", " | Official Music Video"
    Regex("""(?i)\s*[-–—|/]\s*(?:official\s*(?:music\s*|lyric\s*|visualizer\s*)?video|official\s*audio|music\s*video|short\s*version|lyrics?\s*video|video\s*oficial|clip\s*officiel|official\s*visualizer|mv|m/v).*$"""),
    // Explicit single-word or short brackets like (Lyrics), [Audio], [Video], (MV)
    Regex("""(?i)\s*[\(\[\{【（「『]\s*(?:lyrics?|audio|video|visualizer|hd|4k|mv|m/v)\s*[\)\]\}】）」』]""")
)

private val TRAILING_TOPIC_REGEX = Regex("""(?i)\s*-\s*topic\s*$""")
private val DANGLING_SEPARATORS_REGEX = Regex("""\s*[-–—|/:]\s*$""")
private val LEADING_SEPARATORS_REGEX = Regex("""^\s*[-–—|/:]\s*""")
private val MULTI_SPACE_REGEX = Regex("""\s+""")

/**
 * Cleans YouTube upload noise from metadata strings (titles and artist names)
 * before UI display and before querying iTunes / MusicBrainz matchers.
 */
fun cleanMetadataText(raw: String?): String {
    if (raw.isNullOrBlank()) return ""

    var text = raw.trim()

    // 1. Always strip trailing "- Topic" (YouTube auto-generated channel suffix)
    text = TRAILING_TOPIC_REGEX.replace(text, "")

    // 2. Strip non-musical upload-noise tags
    for (pattern in NOISE_PATTERNS) {
        text = pattern.replace(text, "")
    }

    // 3. Clean up dangling separator characters left behind (e.g. "Song Name - ")
    text = DANGLING_SEPARATORS_REGEX.replace(text, "")
    text = LEADING_SEPARATORS_REGEX.replace(text, "")

    // 4. Collapse excess whitespace
    text = MULTI_SPACE_REGEX.replace(text, " ").trim()

    // Fallback to original trimmed text if stripping removed everything
    return text.ifBlank { raw.trim() }
}

/**
 * Extracts and separates pure track title and clean artist name.
 * Handles cases where upload title combines "Artist - Title", "Title - Artist", etc.
 */
fun extractCleanTitleAndArtist(rawTitle: String, rawArtist: String): Pair<String, String> {
    val cleanT = cleanMetadataText(rawTitle)
    val cleanA = cleanMetadataText(rawArtist)

    if (cleanT.isBlank()) return Pair(cleanT, cleanA)

    val compoundSeparator = Regex("""\s+[-–—:|]\s+""")
    if (compoundSeparator.containsMatchIn(cleanT)) {
        val parts = cleanT.split(compoundSeparator, limit = 2).map { it.trim() }
        if (parts.size == 2) {
            val p0 = parts[0]
            val p1 = parts[1]

            // If artist was empty or generic, first part is usually artist and second is title
            if (cleanA.isBlank()) {
                return Pair(p1, p0)
            }

            // Check if left part matches the artist
            val p0Lower = p0.lowercase()
            val aLower = cleanA.lowercase()
            if (p0Lower == aLower || p0Lower.contains(aLower) || aLower.contains(p0Lower)) {
                return Pair(p1, cleanA)
            }

            // Check if right part matches the artist
            val p1Lower = p1.lowercase()
            if (p1Lower == aLower || p1Lower.contains(aLower) || aLower.contains(p1Lower)) {
                return Pair(p0, cleanA)
            }
        }
    }

    return Pair(cleanT, cleanA)
}

/**
 * Normalizes a track title for deduplication in recommendation rails (The Mimikyu Polish).
 * Strips noise descriptors like (Remastered), (Live), (Radio Edit), (Bonus Track),
 * "feat. Artist", punctuation, and collapses to lowercase alphanumeric.
 */
fun normalizeTrackTitleForDeduplication(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    var text = cleanMetadataText(raw).lowercase()
    // Strip parenthesized / bracketed version tags
    text = text.replace(Regex("""(?i)\s*[\(\[\{].*?(remaster|live|radio edit|deluxe|version|edit|bonus|feat|ft\.).*?[\)\]\}]"""), "")
    // Strip trailing feat. / ft. clauses
    text = text.replace(Regex("""(?i)\b(feat\.|ft\.|featuring)\b.*"""), "")
    // Strip non-alphanumeric characters
    text = text.replace(Regex("""[^a-z0-9]"""), "")
    return text.trim()
}
