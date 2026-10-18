package com.lin0721.linmusic.core.player.recommendation

import com.lin0721.linmusic.core.metadata.normalizeTrackTitleForDeduplication
import com.lin0721.linmusic.core.player.QueueItem

/**
 * Post-processing recommendation filters ("The Mimikyu Polish").
 *
 * Implements:
 * 1. The Max-2 Artist Rule:
 *    - At most 2 tracks per artist in a recommendation rail.
 *    - Never place 2 tracks by the same artist consecutively (back-to-back).
 * 2. The 70/30 Familiarity Split:
 *    - Mixes ~70% novel candidate tracks with ~30% familiar favorites (tracks played in the past, but not in the last 3 days).
 * 3. Normalized Title & Artist Deduplication:
 *    - Strips (Remastered), (Live), (Radio Edit), (Bonus Track), etc. to eliminate identical song variants.
 */
object MimikyuPolishFilter {

    /**
     * Applies the complete Mimikyu polish pass to candidate tracks.
     *
     * @param novelCandidates Algorithmic discovery candidates (e.g. from InnerTube RDAMVM radio / Last.fm)
     * @param familiarFavorites High-scoring tracks the user loved previously (not played in the last 3 days)
     * @param targetCount Total desired tracks (e.g. 25-30)
     * @param excludedRecentTitles Normalized titles from the user's immediate recent history (to avoid recommending what they just heard 10 minutes ago)
     */
    fun applyPolish(
        novelCandidates: List<QueueItem>,
        familiarFavorites: List<QueueItem> = emptyList(),
        targetCount: Int = 25,
        excludedRecentTitles: Set<String> = emptySet()
    ): List<QueueItem> {
        val seenNormalizedKeys = excludedRecentTitles.toMutableSet()
        val artistFrequency = mutableMapOf<String, Int>()

        // Target 70% novel (e.g. 18 of 25) and 30% familiar (e.g. 7 of 25)
        val desiredFamiliar = if (familiarFavorites.isNotEmpty()) (targetCount * 0.30f).toInt().coerceAtLeast(1) else 0
        val desiredNovel = targetCount - desiredFamiliar

        val filteredNovel = mutableListOf<QueueItem>()
        val filteredFamiliar = mutableListOf<QueueItem>()

        // 1. Filter novel tracks
        for (item in novelCandidates) {
            if (filteredNovel.size >= desiredNovel) break
            val normKey = getDeduplicationKey(item)
            val primaryArtist = getPrimaryArtist(item.artist)

            if (normKey in seenNormalizedKeys) continue
            if ((artistFrequency[primaryArtist] ?: 0) >= 2) continue

            seenNormalizedKeys.add(normKey)
            artistFrequency[primaryArtist] = (artistFrequency[primaryArtist] ?: 0) + 1
            filteredNovel.add(item)
        }

        // 2. Filter familiar favorites
        for (item in familiarFavorites) {
            if (filteredFamiliar.size >= desiredFamiliar) break
            val normKey = getDeduplicationKey(item)
            val primaryArtist = getPrimaryArtist(item.artist)

            if (normKey in seenNormalizedKeys) continue
            if ((artistFrequency[primaryArtist] ?: 0) >= 2) continue

            seenNormalizedKeys.add(normKey)
            artistFrequency[primaryArtist] = (artistFrequency[primaryArtist] ?: 0) + 1
            filteredFamiliar.add(item)
        }

        // 3. If familiar was smaller than desired, fill remainder with novel
        if (filteredFamiliar.size < desiredFamiliar) {
            for (item in novelCandidates) {
                if (filteredNovel.size + filteredFamiliar.size >= targetCount) break
                val normKey = getDeduplicationKey(item)
                val primaryArtist = getPrimaryArtist(item.artist)

                if (normKey in seenNormalizedKeys) continue
                if ((artistFrequency[primaryArtist] ?: 0) >= 2) continue

                seenNormalizedKeys.add(normKey)
                artistFrequency[primaryArtist] = (artistFrequency[primaryArtist] ?: 0) + 1
                filteredNovel.add(item)
            }
        }

        // 4. Interleave novel and familiar smoothly (e.g. 2 novel, 1 familiar, 2 novel, 1 familiar)
        val interleaved = mutableListOf<QueueItem>()
        var novelIdx = 0
        var famIdx = 0

        while (novelIdx < filteredNovel.size || famIdx < filteredFamiliar.size) {
            // Add 2 novel
            repeat(2) {
                if (novelIdx < filteredNovel.size) {
                    interleaved.add(filteredNovel[novelIdx++])
                }
            }
            // Add 1 familiar
            if (famIdx < filteredFamiliar.size) {
                interleaved.add(filteredFamiliar[famIdx++])
            }
        }

        // 5. Ensure NO back-to-back tracks by the same artist
        return spreadConsecutiveArtists(interleaved)
    }

    /**
     * Prevents any two adjacent tracks from having the same primary artist.
     */
    private fun spreadConsecutiveArtists(tracks: List<QueueItem>): List<QueueItem> {
        if (tracks.size <= 2) return tracks

        val result = mutableListOf<QueueItem>()
        val pending = tracks.toMutableList()

        while (pending.isNotEmpty()) {
            val lastArtist = result.lastOrNull()?.let { getPrimaryArtist(it.artist) }
            val nextItemIdx = pending.indexOfFirst { item ->
                lastArtist == null || getPrimaryArtist(item.artist) != lastArtist
            }

            if (nextItemIdx != -1) {
                result.add(pending.removeAt(nextItemIdx))
            } else {
                // If all remaining have the same artist, append the first one
                result.add(pending.removeAt(0))
            }
        }

        return result
    }

    private fun getDeduplicationKey(item: QueueItem): String {
        val normTitle = normalizeTrackTitleForDeduplication(item.title)
        val primaryArtist = getPrimaryArtist(item.artist).lowercase()
        return "$normTitle|$primaryArtist"
    }

    private fun getPrimaryArtist(artist: String): String {
        return artist.split(",", "/", "&", "feat.", "ft.", "Featuring").firstOrNull()?.trim() ?: artist.trim()
    }
}
