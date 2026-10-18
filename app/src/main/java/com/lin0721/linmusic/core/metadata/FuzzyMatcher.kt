package com.lin0721.linmusic.core.metadata

import kotlin.math.max
import kotlin.math.min

/**
 * Fuzzy matcher for comparing song titles and artists between
 * YouTube Music and iTunes (or Qobuz).
 */
object FuzzyMatcher {

    private val NOISE_PATTERNS = listOf(
        Regex("""(?i)\(official\s*(music)?\s*video\)"""),
        Regex("""(?i)\[official\s*(music)?\s*video\]"""),
        Regex("""(?i)\(official\s*audio\)"""),
        Regex("""(?i)\[official\s*audio\]"""),
        Regex("""(?i)\(lyric\s*video\)"""),
        Regex("""(?i)\[lyric\s*video\]"""),
        Regex("""(?i)\(lyrics\)"""),
        Regex("""(?i)\[lyrics\]"""),
        Regex("""(?i)\(audio\)"""),
        Regex("""(?i)\[audio\]"""),
        Regex("""(?i)\(visualizer\)"""),
        Regex("""(?i)\[visualizer\]"""),
        Regex("""(?i)\(video\)"""),
        Regex("""(?i)\[video\]"""),
        Regex("""(?i)\(hd\)"""),
        Regex("""(?i)\[hd\]"""),
        Regex("""(?i)\(4k\)"""),
        Regex("""(?i)\[4k\]"""),
        Regex("""(?i)\(remaster(ed)?(\s*\d{4})?\)"""),
        Regex("""(?i)\[remaster(ed)?(\s*\d{4})?\]"""),
        Regex("""(?i)\b(feat\.?|ft\.?|featuring)\s+.*$"""),
        Regex("""(?i)\[(feat|ft|featuring)\b.*?\]"""),
        Regex("""(?i)\((feat|ft|featuring)\b.*?\)""")
    )

    private val PUNCTUATION_REGEX = Regex("""[^\p{L}\p{N}\s]""")
    private val MULTI_SPACE_REGEX = Regex("""\s+""")

    /**
     * Clean and normalize a title or artist string.
     */
    fun normalize(input: String): String {
        var text = input.trim()
        for (pattern in NOISE_PATTERNS) {
            text = pattern.replace(text, " ")
        }
        text = PUNCTUATION_REGEX.replace(text, " ")
        text = MULTI_SPACE_REGEX.replace(text, " ").trim().lowercase()
        return text
    }

    /**
     * Compute Levenshtein distance between two strings.
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val d = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) d[i][0] = i
        for (j in 0..s2.length) d[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                d[i][j] = min(
                    min(d[i - 1][j] + 1, d[i][j - 1] + 1),
                    d[i - 1][j - 1] + cost
                )
            }
        }

        return d[s1.length][s2.length]
    }

    /**
     * Levenshtein similarity score between 0.0 and 1.0.
     */
    fun levenshteinSimilarity(s1: String, s2: String): Double {
        val n1 = normalize(s1)
        val n2 = normalize(s2)
        if (n1 == n2) return 1.0
        val maxLen = max(n1.length, n2.length)
        if (maxLen == 0) return 1.0
        val dist = levenshteinDistance(n1, n2)
        return (1.0 - (dist.toDouble() / maxLen)).coerceIn(0.0, 1.0)
    }

    /**
     * Token overlap (Jaccard similarity) between word sets.
     */
    fun tokenJaccardSimilarity(s1: String, s2: String): Double {
        val tokens1 = normalize(s1).split(" ").filter { it.isNotBlank() }.toSet()
        val tokens2 = normalize(s2).split(" ").filter { it.isNotBlank() }.toSet()
        if (tokens1.isEmpty() && tokens2.isEmpty()) return 1.0
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0

        val intersection = tokens1.intersect(tokens2).size
        val union = tokens1.union(tokens2).size
        return intersection.toDouble() / union.toDouble()
    }

    /**
     * Combined similarity score considering both Levenshtein and token overlap.
     */
    fun similarity(s1: String, s2: String): Double {
        val n1 = normalize(s1)
        val n2 = normalize(s2)
        if (n1 == n2) return 1.0
        if (n1.contains(n2) || n2.contains(n1)) {
            val lengthRatio = min(n1.length, n2.length).toDouble() / max(n1.length, n2.length).toDouble()
            return max(0.85, lengthRatio)
        }
        val lev = levenshteinSimilarity(s1, s2)
        val jaccard = tokenJaccardSimilarity(s1, s2)
        return max(lev, jaccard)
    }

    /**
     * Determine if a target candidate matches a query track with high confidence.
     * Returns a confidence score between 0.0 and 1.0, or 0.0 if not matched.
     */
    fun matchConfidence(
        queryTitle: String,
        queryArtist: String,
        candidateTitle: String,
        candidateArtist: String,
        queryAlbum: String? = null,
        candidateAlbum: String? = null,
        queryDurationMs: Long? = null,
        candidateDurationMs: Long? = null
    ): Double {
        val titleSim = similarity(queryTitle, candidateTitle)
        val artistSim = similarity(queryArtist, candidateArtist)

        // Strict thresholds: both title and artist must have good resemblance
        if (titleSim < 0.60 || artistSim < 0.45) {
            return 0.0
        }

        var score = (titleSim * 0.60) + (artistSim * 0.40)

        // Optional album bonus if both albums are known
        if (!queryAlbum.isNullOrBlank() && !candidateAlbum.isNullOrBlank()) {
            val albumSim = similarity(queryAlbum, candidateAlbum)
            if (albumSim >= 0.70) {
                score = min(1.0, score + 0.10)
            }
        }

        // Optional duration sanity check if candidate duration is provided
        if (queryDurationMs != null && queryDurationMs > 0 &&
            candidateDurationMs != null && candidateDurationMs > 0) {
            val diffSeconds = kotlin.math.abs(queryDurationMs - candidateDurationMs) / 1000
            if (diffSeconds > 30) {
                // Large discrepancy in duration (e.g. full live concert or medley vs single)
                score -= 0.25
            } else if (diffSeconds <= 5) {
                score = min(1.0, score + 0.05)
            }
        }

        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Check if confidence score meets the threshold for an accepted enrichment match.
     */
    fun isConfidentMatch(confidence: Double): Boolean {
        return confidence >= 0.70
    }
}
