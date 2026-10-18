package com.lin0721.linmusic.core.player.recommendation

import androidx.compose.ui.graphics.Color
import com.lin0721.linmusic.core.lastfm.LastFmRepository
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.player.QueueItem
import com.lin0721.linmusic.core.ui.components.getMimikyuPastelColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

private const val TAG = "TasteClusterEngine"

/**
 * A cluster or "Taste Bucket" representing a distinct vibe/genre/artist affinity group.
 */
data class TasteCluster(
    val id: String,
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val seedTracks: List<ScoredTrackRecord>,
    val seedArtists: List<String>
)

/**
 * Clusters user listening history into 3 to 5 distinct Taste Buckets before fetching recommendations.
 * Prevents chaotic blending of mutually exclusive genres (e.g. Lo-Fi study beats vs gym metal).
 */
class TasteClusterEngine(
    private val lastFmRepository: LastFmRepository = LastFmRepository()
) {

    // Canonical Genre/Vibe Definitions with keyword matchers
    private enum class VibeCategory(
        val displayName: String,
        val subtitlePrefix: String,
        val defaultColor: Color,
        val keywords: Set<String>
    ) {
        POP_DANCE(
            "Pop & Dance Mix",
            "Upbeat hits & modern pop",
            Color(0xFFE91E63),
            setOf("pop", "dance", "electropop", "synthpop", "chart", "europop", "disco")
        ),
        HIPHOP_RNB(
            "Hip-Hop & R&B Mix",
            "Beats, flow & soul",
            Color(0xFFFF9800),
            setOf("hip-hop", "rap", "trap", "r&b", "soul", "contemporary r&b", "urban")
        ),
        ROCK_INDIE(
            "Indie & Rock Mix",
            "Alternative riffs & indie anthems",
            Color(0xFF9C27B0),
            setOf("rock", "indie", "alternative", "indie rock", "classic rock", "grunge", "punk")
        ),
        CHILL_ACOUSTIC(
            "Chill & Lo-Fi Mix",
            "Laid-back vibes & mellow acoustics",
            Color(0xFF009688),
            setOf("chill", "lo-fi", "ambient", "acoustic", "singer-songwriter", "folk", "downtempo")
        ),
        ELECTRONIC_CLUB(
            "Electronic & Energy Mix",
            "BPM, synth & late night electronic",
            Color(0xFF2196F3),
            setOf("electronic", "house", "edm", "techno", "trance", "drum and bass", "electro")
        )
    }

    /**
     * Clusters eligible scored tracks into 3 to 5 distinct taste buckets.
     */
    suspend fun clusterHistory(
        eligibleSeeds: List<ScoredTrackRecord>,
        topArtistCount: Int = 10
    ): List<TasteCluster> = withContext(Dispatchers.IO) {
        if (eligibleSeeds.isEmpty()) {
            return@withContext emptyList()
        }

        // 1. Tag each seed with its genre/vibe
        val candidateSeeds = eligibleSeeds.take(50)
        val taggedSeeds = coroutineScope {
            candidateSeeds.map { seed ->
                async {
                    val genre = resolveGenreForArtist(seed.artist)
                    seed to genre
                }
            }.awaitAll()
        }

        // 2. Bucket tracks into Vibe Categories
        val bucketMap = mutableMapOf<VibeCategory, MutableList<ScoredTrackRecord>>()
        for ((seed, genre) in taggedSeeds) {
            val matchedCategory = VibeCategory.entries.firstOrNull { cat ->
                cat.keywords.any { keyword -> genre.contains(keyword, ignoreCase = true) }
            } ?: VibeCategory.POP_DANCE // Default fallback

            bucketMap.getOrPut(matchedCategory) { mutableListOf() }.add(seed)
        }

        val resultClusters = mutableListOf<TasteCluster>()

        // 3. Check for heavy single-artist affinity ("Because you listen to [Artist]")
        val artistGroup = eligibleSeeds.groupBy { it.artist.split(",", "/", "&").firstOrNull()?.trim() ?: it.artist }
            .filter { it.value.size >= 2 }
            .maxByOrNull { entry -> entry.value.sumOf { it.score } }

        if (artistGroup != null) {
            val topArtist = artistGroup.key
            val artistSeeds = artistGroup.value.sortedByDescending { it.score }.take(3)
            resultClusters.add(
                TasteCluster(
                    id = "artist_${topArtist.hashCode()}",
                    title = "Because you listen to $topArtist",
                    subtitle = "Inspired by your recent love for $topArtist",
                    primaryColor = getMimikyuPastelColor(topArtist),
                    seedTracks = artistSeeds,
                    seedArtists = listOf(topArtist)
                )
            )
        }

        // 4. Convert active genre buckets with at least 1 seed into distinct clusters
        val sortedBuckets = bucketMap.entries
            .sortedByDescending { it.value.sumOf { track -> track.score } }
            .take(4)

        for ((index, entry) in sortedBuckets.withIndex()) {
            val category = entry.key
            val seeds = entry.value.sortedByDescending { it.score }.take(3)
            val artists = seeds.map { it.artist }.distinct().take(3)
            val clusterId = "cluster_${category.name.lowercase()}"

            // Skip if identical to artist cluster
            if (resultClusters.any { it.id == clusterId }) continue

            val subtitle = if (artists.isNotEmpty()) {
                "${category.subtitlePrefix}: ${artists.joinToString(", ")}"
            } else {
                category.subtitlePrefix
            }

            resultClusters.add(
                TasteCluster(
                    id = clusterId,
                    title = category.displayName,
                    subtitle = subtitle,
                    primaryColor = category.defaultColor,
                    seedTracks = seeds,
                    seedArtists = artists
                )
            )
        }

        AppLogger.i(TAG, "Clustered user history into ${resultClusters.size} distinct taste buckets")
        resultClusters
    }

    private suspend fun resolveGenreForArtist(artist: String): String {
        val primary = artist.split(",", "/", "&").firstOrNull()?.trim() ?: artist.trim()
        if (primary.isBlank()) return "pop"

        return try {
            val info = lastFmRepository.getArtistInfo(primary)
            val tag = info?.topTag?.takeIf { it.isNotBlank() }
                ?: info?.tags?.firstOrNull()
                ?: ""
            tag.lowercase()
        } catch (e: Exception) {
            "pop"
        }
    }
}
