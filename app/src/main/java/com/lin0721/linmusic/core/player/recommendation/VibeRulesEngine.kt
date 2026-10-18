package com.lin0721.linmusic.core.player.recommendation

import androidx.compose.ui.graphics.Color
import com.lin0721.linmusic.core.metadata.ArtistCutoutProvider
import com.lin0721.linmusic.core.player.QueueItem
import com.lin0721.linmusic.core.ui.components.ThisIsCoverSpec
import com.lin0721.linmusic.feature.recent.domain.RecentSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Rules Engine for Mimikyu-tier "Made For Your Vibe" personal shelves.
 * Runs behavioral heuristics on listening history and implicit interaction scores:
 *
 * 1. #1 most replayed artist -> "MY THERAPIST" (Pastel Sky Blue #AAE2FF)
 * 2. Late-night plays (12 AM - 4 AM) -> "3AM OVERTHINKING" (Cyan-to-Ice Gradient #00B4DB -> #B2FEFA)
 * 3. Obsessive looping / session replays -> "MY ROMAN EMPIRE" (Mimikyu Amber Yellow #FFB300)
 * 4. Top female vocalists cluster -> "MY TASTE IN WOMEN" (Electric Blue #00A6FF)
 * 5. High-BPM / High-Energy / 5PM-8PM -> "PURE CAFFEINE" (Neon Coral #FF5252)
 * 6. Nostalgia / Throwbacks -> "MY PERSONALITY" (Mint Green #00E676)
 */
object VibeRulesEngine {

    const val VIBE_THERAPIST_ID = -501L
    const val VIBE_LATE_NIGHT_ID = -502L
    const val VIBE_ROMAN_EMPIRE_ID = -503L
    const val VIBE_FEMALE_VOCALISTS_ID = -504L
    const val VIBE_CAFFEINE_ID = -505L
    const val VIBE_PERSONALITY_ID = -506L

    private val FEMALE_VOCALISTS = setOf(
        "taylor swift", "sza", "dua lipa", "billie eilish", "lana del rey",
        "ariana grande", "olivia rodrigo", "rihanna", "beyoncé", "beyonce",
        "sabrina carpenter", "chappell roan", "charli xcx", "adele", "doja cat",
        "rosalía", "rosalia", "katy perry", "lady gaga", "selena gomez"
    )

    private val NOSTALGIA_ARTISTS = setOf(
        "michael jackson", "queen", "nirvana", "the beatles", "eminem",
        "linkin park", "green day", "britney spears", "oasis", "daft punk",
        "coldplay", "radiohead", "red hot chili peppers", "ac/dc", "metallica"
    )

    private val HIGH_ENERGY_KEYWORDS = setOf(
        "trap", "edm", "house", "techno", "rock", "metal", "drill",
        "phonk", "rave", "fast", "hype", "club", "dance", "electronic"
    )

    /**
     * Evaluates listening behavior and returns generated Vibe Cover Specs.
     */
    suspend fun generateVibeShelves(
        history: List<RecentSong>,
        scoredSeeds: List<ScoredTrackRecord>,
        onRepeatTracks: List<ScoredTrackRecord>
    ): List<ThisIsCoverSpec> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ThisIsCoverSpec>()

        // 1. RULE: MY THERAPIST (#1 most replayed artist overall)
        val topArtist = determineTopReplayedArtist(history, scoredSeeds)
        if (topArtist != null) {
            val cutout = ArtistCutoutProvider.getArtistCutout(topArtist)
            results.add(
                ThisIsCoverSpec(
                    id = "vibe_therapist",
                    headline = "MY THERAPIST",
                    subtitle = "You've had $topArtist on heavy rotation",
                    artistCutoutUrlOrBitmap = cutout.ifBlank { null },
                    bottomColorStart = Color(0xFFAAE2FF),
                    bottomColorEnd = Color(0xFFAAE2FF),
                    virtualId = VIBE_THERAPIST_ID,
                    artistName = topArtist
                )
            )
        }

        // 2. RULE: 3AM OVERTHINKING (Late-night listening between 12 AM and 4 AM)
        val lateNightArtist = determineLateNightArtist(history, scoredSeeds)
        if (lateNightArtist != null) {
            val cutout = ArtistCutoutProvider.getArtistCutout(lateNightArtist)
            results.add(
                ThisIsCoverSpec(
                    id = "vibe_3am",
                    headline = "3AM OVERTHINKING",
                    subtitle = "For midnight thoughts and late hours with $lateNightArtist",
                    artistCutoutUrlOrBitmap = cutout.ifBlank { null },
                    bottomColorStart = Color(0xFF00B4DB),
                    bottomColorEnd = Color(0xFFB2FEFA),
                    virtualId = VIBE_LATE_NIGHT_ID,
                    artistName = lateNightArtist
                )
            )
        }

        // 3. RULE: MY ROMAN EMPIRE (Obsessive looping / session replays)
        val loopedTrack = onRepeatTracks.firstOrNull() ?: scoredSeeds.firstOrNull { it.playCount >= 2 }
        val loopedArtist = loopedTrack?.artist?.split(",", "/", "&")?.firstOrNull()?.trim()
            ?: history.groupBy { it.title }.maxByOrNull { it.value.size }?.value?.firstOrNull()?.artist
        if (loopedArtist != null) {
            val cutout = ArtistCutoutProvider.getArtistCutout(loopedArtist)
            results.add(
                ThisIsCoverSpec(
                    id = "vibe_roman_empire",
                    headline = "MY ROMAN EMPIRE",
                    subtitle = "The songs you simply can't stop replaying",
                    artistCutoutUrlOrBitmap = cutout.ifBlank { null },
                    bottomColorStart = Color(0xFFFFB300),
                    bottomColorEnd = Color(0xFFFFB300),
                    virtualId = VIBE_ROMAN_EMPIRE_ID,
                    artistName = loopedArtist
                )
            )
        }

        // 4. RULE: MY TASTE IN WOMEN / MAIN CHARACTER (Female vocalists cluster)
        val femaleVocalist = determineTopFemaleVocalist(history, scoredSeeds)
        if (femaleVocalist != null) {
            val cutout = ArtistCutoutProvider.getArtistCutout(femaleVocalist)
            results.add(
                ThisIsCoverSpec(
                    id = "vibe_taste_in_women",
                    headline = "MY TASTE IN WOMEN",
                    subtitle = "The voices defining your rotation right now",
                    artistCutoutUrlOrBitmap = cutout.ifBlank { null },
                    bottomColorStart = Color(0xFF00A6FF),
                    bottomColorEnd = Color(0xFF00A6FF),
                    virtualId = VIBE_FEMALE_VOCALISTS_ID,
                    artistName = femaleVocalist
                )
            )
        }

        // 5. RULE: PURE CAFFEINE (High-BPM / High-Energy / 5PM-8PM workout)
        val energyArtist = determineHighEnergyArtist(history, scoredSeeds)
        if (energyArtist != null) {
            val cutout = ArtistCutoutProvider.getArtistCutout(energyArtist)
            results.add(
                ThisIsCoverSpec(
                    id = "vibe_pure_caffeine",
                    headline = "PURE CAFFEINE",
                    subtitle = "Instant adrenaline & high-energy anthems",
                    artistCutoutUrlOrBitmap = cutout.ifBlank { null },
                    bottomColorStart = Color(0xFFFF5252),
                    bottomColorEnd = Color(0xFFFF5252),
                    virtualId = VIBE_CAFFEINE_ID,
                    artistName = energyArtist
                )
            )
        }

        // 6. RULE: MY PERSONALITY (Nostalgia cluster)
        val nostalgiaArtist = determineNostalgiaArtist(history, scoredSeeds)
        if (nostalgiaArtist != null) {
            val cutout = ArtistCutoutProvider.getArtistCutout(nostalgiaArtist)
            results.add(
                ThisIsCoverSpec(
                    id = "vibe_my_personality",
                    headline = "MY PERSONALITY",
                    subtitle = "Throwbacks and classics you'll never let go of",
                    artistCutoutUrlOrBitmap = cutout.ifBlank { null },
                    bottomColorStart = Color(0xFF00E676),
                    bottomColorEnd = Color(0xFF00E676),
                    virtualId = VIBE_PERSONALITY_ID,
                    artistName = nostalgiaArtist
                )
            )
        }

        results
    }

    private fun determineTopReplayedArtist(
        history: List<RecentSong>,
        scoredSeeds: List<ScoredTrackRecord>
    ): String? {
        val scoredTop = scoredSeeds.maxByOrNull { it.score * 2 + it.playCount }?.artist
        if (!scoredTop.isNullOrBlank()) {
            return scoredTop.split(",", "/", "&").firstOrNull()?.trim()
        }
        val historyCounts = history.groupingBy {
            it.artist.split(",", "/", "&").firstOrNull()?.trim() ?: it.artist
        }.eachCount()
        return historyCounts.maxByOrNull { it.value }?.key ?: "The Weeknd"
    }

    private fun determineLateNightArtist(
        history: List<RecentSong>,
        scoredSeeds: List<ScoredTrackRecord>
    ): String? {
        val calendar = Calendar.getInstance()
        val lateNightPlays = history.filter { play ->
            calendar.timeInMillis = play.playTime
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hour in 0..4 // 12:00 AM to 4:59 AM
        }
        if (lateNightPlays.isNotEmpty()) {
            val counts = lateNightPlays.groupingBy {
                it.artist.split(",", "/", "&").firstOrNull()?.trim() ?: it.artist
            }.eachCount()
            return counts.maxByOrNull { it.value }?.key
        }
        // Fallback to chill/lo-fi/indie artist in history or curated icon
        val chillCandidate = (scoredSeeds.map { it.artist } + history.map { it.artist })
            .firstOrNull { a ->
                val lower = a.lowercase()
                lower.contains("frank ocean") || lower.contains("lana del rey") ||
                    lower.contains("joji") || lower.contains("sza") || lower.contains("billie eilish")
            }
        return chillCandidate?.split(",", "/", "&")?.firstOrNull()?.trim() ?: "Frank Ocean"
    }

    private fun determineTopFemaleVocalist(
        history: List<RecentSong>,
        scoredSeeds: List<ScoredTrackRecord>
    ): String? {
        val allArtists = (scoredSeeds.map { it.artist } + history.map { it.artist })
            .flatMap { it.split(",", "/", "&") }
            .map { it.trim() }

        val found = allArtists.firstOrNull { art ->
            FEMALE_VOCALISTS.any { fem -> art.lowercase().contains(fem) }
        }
        return found ?: "Taylor Swift"
    }

    private fun determineHighEnergyArtist(
        history: List<RecentSong>,
        scoredSeeds: List<ScoredTrackRecord>
    ): String? {
        // Check plays between 5 PM and 8 PM (17:00 - 20:00)
        val calendar = Calendar.getInstance()
        val workoutPlays = history.filter { play ->
            calendar.timeInMillis = play.playTime
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hour in 17..20
        }
        if (workoutPlays.isNotEmpty()) {
            val counts = workoutPlays.groupingBy {
                it.artist.split(",", "/", "&").firstOrNull()?.trim() ?: it.artist
            }.eachCount()
            val candidate = counts.maxByOrNull { it.value }?.key
            if (!candidate.isNullOrBlank()) return candidate
        }

        val hypeArtist = (scoredSeeds.map { it.artist } + history.map { it.artist })
            .firstOrNull { a ->
                val lower = a.lowercase()
                lower.contains("travis scott") || lower.contains("kanye west") ||
                    lower.contains("drake") || lower.contains("eminem") || lower.contains("carti")
            }
        return hypeArtist?.split(",", "/", "&")?.firstOrNull()?.trim() ?: "Travis Scott"
    }

    private fun determineNostalgiaArtist(
        history: List<RecentSong>,
        scoredSeeds: List<ScoredTrackRecord>
    ): String? {
        val allArtists = (scoredSeeds.map { it.artist } + history.map { it.artist })
            .flatMap { it.split(",", "/", "&") }
            .map { it.trim() }

        val found = allArtists.firstOrNull { art ->
            NOSTALGIA_ARTISTS.any { old -> art.lowercase().contains(old) }
        }
        return found ?: "Michael Jackson"
    }
}
