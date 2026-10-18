package com.lin0721.linmusic.core.player.recommendation

import androidx.compose.ui.graphics.Color
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lin0721.linmusic.core.lastfm.LastFmRepository
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.metadata.cleanMetadataText
import com.lin0721.linmusic.core.player.QueueItem
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import com.lin0721.linmusic.core.ui.components.MimikyuCoverType
import com.lin0721.linmusic.core.ui.components.getMimikyuPastelColor
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.WatchEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "DailyMixGenerator"

/**
 * A single generated Mimikyu-style mix, station, or playlist.
 */
data class DailyMixPlaylist(
    /** Virtual playlist ID */
    val id: Long,
    val title: String,
    val subtitle: String,
    val coverUrl: String,
    val coverType: MimikyuCoverType = MimikyuCoverType.IMAGE_URL,
    val primaryColor: Color = Color(0xFF1DB954),
    val mosaicImages: List<String> = emptyList(),
    val tracks: List<QueueItem> = emptyList(),
    val isArtistProfile: Boolean = false,
    val artistId: Long? = null,
    val seedQuery: String = "",
    val seedVideoId: String = ""
)

data class MimikyuShelvesResult(
    val topMixes: List<DailyMixPlaylist>,
    val recommendedStations: List<DailyMixPlaylist>,
    val bestOfArtists: List<DailyMixPlaylist>,
    val favoriteArtists: List<DailyMixPlaylist>,
    val moreLikeMixes: List<DailyMixPlaylist>,
    val moreLikeArtistName: String,
    val moreLikeArtistCover: String,
    val madeForYourVibe: List<com.lin0721.linmusic.core.ui.components.ThisIsCoverSpec> = emptyList()
)

/**
 * Generates Mimikyu-authentic Home sections:
 * 1. "Your top mixes" (7-10 playlists: On Repeat, Daily Mixes 1-3, Pop Mix, Chill Mix, Happy Mix, Indie Mix, Rock Mix, R&B Mix, 90s Mix)
 * 2. "Recommended Stations" (7-10 artist radios with pastel backgrounds, 3 circular artist cutouts, RADIO tag)
 * 3. "Made for your vibe" (6 personal behavioral vibe cards: MY THERAPIST, 3AM OVERTHINKING, MY ROMAN EMPIRE, etc.)
 * 4. "Best of artists" (7-10 "THIS IS [Artist]" playlists)
 * 5. "Your favourite artists" (7-10 circular artist cards)
 * 6. "More like [Artist]" (7-10 items featuring user's top artist)
 *
 * Upgraded to Mimikyu-level quality:
 * - Multi-Signal Taste Clustering (distinct genre/vibe buckets)
 * - InnerTube Native "Up Next" RDAMVM Radio Engine
 * - Implicit User Signal Weighting (skips, full listens, replays, likes)
 * - Mimikyu Polish Filter (Max-2 artist rule, 70/30 novelty/familiarity split, normalized title deduplication)
 * - VibeRulesEngine (Meme-style behavioral titles with transparent artist cutouts)
 */
class DailyMixGenerator(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val lastFmRepository: LastFmRepository = LastFmRepository(),
    private val tasteSignalScorer: TasteSignalScorer? = null,
    private val playbackHistoryRepository: com.lin0721.linmusic.core.history.PlaybackHistoryRepository? = null
) {

    companion object {
        const val ON_REPEAT_ID = -9L
        const val DAILY_MIX_1_ID = -10L
        const val DAILY_MIX_2_ID = -11L
        const val DAILY_MIX_3_ID = -12L

        private const val TRACKS_PER_MIX = 30
        private const val TOP_ARTISTS_FETCH = 20

        private val cachedMixes = ConcurrentHashMap<Long, DailyMixPlaylist>()

        fun getCachedMix(id: Long): DailyMixPlaylist? {
            val normalizedId = when (id) {
                -1L -> DAILY_MIX_1_ID
                -2L -> DAILY_MIX_2_ID
                -3L -> DAILY_MIX_3_ID
                else -> id
            }
            return cachedMixes[normalizedId]
        }

        fun cacheMix(mix: DailyMixPlaylist) {
            cachedMixes[mix.id] = mix
        }

        val FALLBACK_SEED_VIDEO_IDS = listOf(
            "OPf0YbXqDm0",  // Mark Ronson - Uptown Funk
            "X-TiHfM0t5E",  // Taylor Swift - Anti-Hero
            "YQHsXMglC9A",  // Adele - Hello
            "JGwWNGJdvx8",  // Ed Sheeran - Shape of You
            "3tmd-ClpJxA",  // Taylor Swift - Look What You Made Me Do
            "kJQP7kiw5Fk",  // Luis Fonsi - Despacito
            "2Vv-BfVoq4g",  // Ed Sheeran - Perfect
            "09R8_2nJtjg",  // Maroon 5 - Sugar
            "hT_nvWreIhg"   // OneRepublic - Counting Stars
        )

        val CURATED_ARTISTS = listOf(
            "The Weeknd",
            "Kendrick Lamar",
            "Taylor Swift",
            "Drake",
            "Bruno Mars",
            "SZA",
            "Billie Eilish",
            "Travis Scott",
            "Dua Lipa",
            "Coldplay",
            "Tame Impala",
            "Frank Ocean",
            "Kanye West",
            "Arijit Singh",
            "Diljit Dosanjh",
            "Chaar Diwaari",
            "Seedhe Maut",
            "Michael Jackson",
            "Post Malone",
            "Arctic Monkeys"
        )

        /**
         * Normalizes an artist name to a canonical deduplication key to prevent
         * duplicates like "The Jackson 5", "Jackson 5", "The Jacksons", or feature credits.
         */
        fun canonicalArtistKey(artist: String): String {
            if (artist.isBlank()) return ""
            var key = artist.trim().lowercase()
            if (key.startsWith("the ")) {
                key = key.removePrefix("the ").trim()
            }
            // Strip collaboration/feature tails
            key = key.split(Regex("\\s+(feat\\.|ft\\.|with|vs\\.|&|/)\\s+")).first().trim()
            val alphaNumeric = key.replace(Regex("[^a-z0-9]"), "")
            // Map variations of The Jackson 5 / The Jacksons to a single canonical entity
            if (alphaNumeric.contains("jackson") && (alphaNumeric.contains("5") || alphaNumeric.contains("five") || alphaNumeric.contains("jacksons") || alphaNumeric == "jacksons")) {
                return "jackson5"
            }
            if (alphaNumeric == "michaeljackson") {
                return "michaeljackson"
            }
            return alphaNumeric
        }
    }

    /**
     * Generates all 5 Mimikyu shelves with 7-10 items each.
     */
    suspend fun generateAllMimikyuShelves(): MimikyuShelvesResult = withContext(Dispatchers.IO) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val firestoreSeeds = if (uid != null) fetchUserTopArtists(uid) else emptyList()
        val eligibleTasteSeeds = tasteSignalScorer?.getEligibleSeedTracks() ?: emptyList()
        val familiarFavorites = tasteSignalScorer?.getFamiliarFavorites() ?: emptyList()
        val onRepeatTracks = tasteSignalScorer?.getOnRepeatTracks() ?: emptyList()

        // Combine user seeds prioritizing high-affinity scored tracks
        val userSeeds = if (eligibleTasteSeeds.isNotEmpty()) {
            val fromTaste = eligibleTasteSeeds.map { it.artist to (it.videoId.ifBlank { it.songId.toString() }) }
            (fromTaste + firestoreSeeds).distinctBy { it.first }
        } else {
            firestoreSeeds
        }

        // Establish the top artists pool: user's history first, augmented by Last.fm and curated icons
        val userArtists = userSeeds.map { it.first }
            .filter { it.isNotBlank() }
            .distinctBy { canonicalArtistKey(it) }

        val primaryArtist = userArtists.firstOrNull() ?: CURATED_ARTISTS.first()

        // Last.fm similar artists for primary artist - strictly deduplicated & capped at 3 so they don't flood the shelf
        val lastFmSimilar = runCatching { lastFmRepository.getSimilarArtists(primaryArtist, limit = 10) }
            .getOrDefault(emptyList())
            .filter { it.isNotBlank() }
            .distinctBy { canonicalArtistKey(it) }
            .filter { canonicalArtistKey(it) != canonicalArtistKey(primaryArtist) }
            .take(3)

        // Merge intelligently: User artists first + up to 3 similar artists + diverse curated artists
        // Ensure every artist only appears exactly ONCE across all recommendations
        val seenEnrichedKeys = mutableSetOf<String>()
        val combinedCandidates = (userArtists.ifEmpty { listOf(primaryArtist) } + lastFmSimilar + CURATED_ARTISTS)
        val enrichedArtists = combinedCandidates.filter { artist ->
            val key = canonicalArtistKey(artist)
            key.isNotBlank() && seenEnrichedKeys.add(key)
        }.take(14)

        // Preload portraits for all top and similar artists in parallel
        val allArtistsToResolve = (enrichedArtists + lastFmSimilar).distinct()
        coroutineScope {
            allArtistsToResolve.map { artist ->
                async {
                    runCatching {
                        com.lin0721.linmusic.core.metadata.ArtistPortraitProvider.getArtistPortrait(artist)
                    }
                }
            }.awaitAll()
        }

        // 1. Build "Your top mixes" (On Repeat, Clustered Daily Mixes 1-3, Vibe Mixes)
        val topMixes = buildTopMixes(userSeeds, eligibleTasteSeeds, familiarFavorites, onRepeatTracks)
        topMixes.forEach { cacheMix(it) }

        // 2. Build "Recommended Stations" (7-10 artist radios)
        val stations = buildRecommendedStations(enrichedArtists, lastFmSimilar)
        stations.forEach { cacheMix(it) }

        // 3. Build "Made for your vibe" (6 personal behavioral vibe cards with artist cutouts)
        val history = playbackHistoryRepository?.plays?.value ?: emptyList()
        val madeForYourVibe = VibeRulesEngine.generateVibeShelves(history, eligibleTasteSeeds, onRepeatTracks)
        madeForYourVibe.forEach { spec ->
            cacheMix(
                DailyMixPlaylist(
                    id = spec.virtualId,
                    title = "This Is " + spec.headline.lowercase().replaceFirstChar { it.uppercase() },
                    subtitle = spec.subtitle,
                    coverUrl = (spec.artistCutoutUrlOrBitmap as? String).orEmpty(),
                    coverType = MimikyuCoverType.THIS_IS,
                    primaryColor = spec.bottomColorStart,
                    seedQuery = "${spec.headline} ${spec.artistName} hits"
                )
            )
        }

        // 4. Build "Best of artists" (7-10 "THIS IS [Artist]" playlists)
        val bestOf = buildBestOfArtists(enrichedArtists)
        bestOf.forEach { cacheMix(it) }

        // 5. Build "Your favourite artists" (7-10 circular artist cards)
        val favoriteArtists = buildFavoriteArtists(enrichedArtists)
        favoriteArtists.forEach { cacheMix(it) }

        // 6. Build "More like [Artist]" shelf (7-10 mixed items)
        val moreLike = buildMoreLikeShelf(primaryArtist, lastFmSimilar)
        moreLike.forEach { cacheMix(it) }

        val primaryArtistCover = com.lin0721.linmusic.core.metadata.ArtistPortraitProvider.getCachedPortrait(primaryArtist)
            .ifBlank { stations.firstOrNull { it.title.contains(primaryArtist, ignoreCase = true) }?.coverUrl ?: "" }

        MimikyuShelvesResult(
            topMixes = topMixes,
            recommendedStations = stations,
            bestOfArtists = bestOf,
            favoriteArtists = favoriteArtists,
            moreLikeMixes = moreLike,
            moreLikeArtistName = primaryArtist,
            moreLikeArtistCover = primaryArtistCover,
            madeForYourVibe = madeForYourVibe
        )
    }

    /**
     * Backward-compatible helper for HomeViewModel.
     */
    suspend fun generateDailyMixes(): List<DailyMixPlaylist> {
        val all = generateAllMimikyuShelves()
        return all.topMixes
    }

    private suspend fun buildTopMixes(
        userSeeds: List<Pair<String, String>>,
        eligibleTasteSeeds: List<ScoredTrackRecord>,
        familiarFavorites: List<ScoredTrackRecord>,
        onRepeatTracks: List<ScoredTrackRecord>
    ): List<DailyMixPlaylist> = coroutineScope {
        val mixes = mutableListOf<DailyMixPlaylist>()

        // 0. "On Repeat" shelf if user has repeat listens or high scores
        if (onRepeatTracks.isNotEmpty()) {
            val repeatItems = onRepeatTracks.map {
                QueueItem(
                    songId = it.songId,
                    title = it.title,
                    artist = it.artist,
                    coverUrl = it.coverUrl
                )
            }
            val onRepeatMix = DailyMixPlaylist(
                id = ON_REPEAT_ID,
                title = "On Repeat",
                subtitle = "Songs you've been playing again and again",
                coverUrl = repeatItems.firstOrNull()?.coverUrl ?: "",
                coverType = MimikyuCoverType.IMAGE_URL,
                primaryColor = Color(0xFF1DB954),
                tracks = repeatItems
            )
            mixes.add(onRepeatMix)
        }

        // 1. Daily Mix 1-3 using Taste Clustering + InnerTube RDAMVM Radio Engine
        val tasteClusters = if (eligibleTasteSeeds.isNotEmpty()) {
            TasteClusterEngine(lastFmRepository).clusterHistory(eligibleTasteSeeds)
        } else emptyList()

        val familiarQueueItems = familiarFavorites.map {
            QueueItem(
                songId = it.songId,
                title = it.title,
                artist = it.artist,
                coverUrl = it.coverUrl
            )
        }

        val mixLabels = listOf("Daily Mix 1", "Daily Mix 2", "Daily Mix 3")
        val defaultColors = listOf(Color(0xFFE91E63), Color(0xFF2196F3), Color(0xFF4CAF50))
        val fallbackSeeds = if (userSeeds.isNotEmpty()) userSeeds.map { it.second } else FALLBACK_SEED_VIDEO_IDS

        for (i in 0 until 3) {
            val id = -10L - i
            val cluster = tasteClusters.getOrNull(i)
            val seedVideoId = cluster?.seedTracks?.firstOrNull()?.videoId?.takeIf { it.isNotBlank() }
                ?: fallbackSeeds.getOrNull(i)
                ?: FALLBACK_SEED_VIDEO_IDS[i % FALLBACK_SEED_VIDEO_IDS.size]

            val mix = buildMixFromSeed(
                id = id,
                mixLabel = mixLabels[i],
                subtitleOverride = cluster?.subtitle,
                seedVideoId = seedVideoId,
                trackCount = TRACKS_PER_MIX,
                coverType = MimikyuCoverType.IMAGE_URL,
                primaryColor = cluster?.primaryColor ?: defaultColors[i],
                familiarFavorites = familiarQueueItems
            )
            if (mix != null) mixes.add(mix)
        }

        // Vibe Mixes: Pop, Chill, Happy, Indie, Rock, R&B, 90s
        val vibeConfigs = listOf(
            Triple(-13L, "Pop Mix", "Dua Lipa"),
            Triple(-14L, "Chill Mix", "Frank Ocean"),
            Triple(-15L, "Happy Mix", "Bruno Mars"),
            Triple(-16L, "Indie Mix", "Tame Impala"),
            Triple(-17L, "Rock Mix", "Coldplay"),
            Triple(-18L, "R&B Mix", "SZA"),
            Triple(-19L, "90s Mix", "Michael Jackson")
        )

        for ((id, title, seedArtist) in vibeConfigs) {
            val color = getMimikyuPastelColor(title)
            val artistCover = com.lin0721.linmusic.core.metadata.ArtistPortraitProvider.getCachedPortrait(seedArtist)

            mixes.add(
                DailyMixPlaylist(
                    id = id,
                    title = title,
                    subtitle = "Personalized for you",
                    coverUrl = artistCover,
                    coverType = MimikyuCoverType.IMAGE_URL,
                    primaryColor = color,
                    seedQuery = "$title $seedArtist hits"
                )
            )
        }

        mixes
    }

    private suspend fun buildRecommendedStations(
        artists: List<String>,
        lastFmSimilar: List<String>
    ): List<DailyMixPlaylist> {
        val stationList = mutableListOf<DailyMixPlaylist>()
        val distinctArtists = artists.take(10)

        for ((index, artist) in distinctArtists.withIndex()) {
            val id = -100L - index
            val color = getMimikyuPastelColor(artist)

            // Discover 3 related artists for subtitle & side avatars
            val similar = if (index == 0 && lastFmSimilar.isNotEmpty()) {
                lastFmSimilar.take(3)
            } else {
                runCatching { lastFmRepository.getSimilarArtists(artist, limit = 3) }.getOrDefault(emptyList())
            }

            val subtitle = if (similar.isNotEmpty()) {
                "$artist, ${similar.joinToString(", ")} & more"
            } else {
                "$artist Radio"
            }

            val centerCover = com.lin0721.linmusic.core.metadata.ArtistPortraitProvider.getCachedPortrait(artist)
            val sideAvatars = similar.map { com.lin0721.linmusic.core.metadata.ArtistPortraitProvider.getCachedPortrait(it) }

            stationList.add(
                DailyMixPlaylist(
                    id = id,
                    title = artist,
                    subtitle = subtitle,
                    coverUrl = centerCover,
                    coverType = MimikyuCoverType.IMAGE_URL,
                    primaryColor = color,
                    mosaicImages = sideAvatars,
                    seedQuery = "$artist radio"
                )
            )
        }

        return stationList
    }

    private fun buildBestOfArtists(artists: List<String>): List<DailyMixPlaylist> {
        val seenKeys = mutableSetOf<String>()
        return artists.filter { it.isNotBlank() }
            .filter { artist ->
                val key = canonicalArtistKey(artist)
                key.isNotBlank() && seenKeys.add(key)
            }
            .take(10).mapIndexed { index, artist ->
            val cover = com.lin0721.linmusic.core.metadata.ArtistPortraitProvider.getCachedPortrait(artist)
            DailyMixPlaylist(
                id = -200L - index,
                title = artist,
                subtitle = "This is $artist. The essential tracks, all in one playlist.",
                coverUrl = cover,
                coverType = MimikyuCoverType.IMAGE_URL,
                primaryColor = getMimikyuPastelColor(artist),
                seedQuery = "$artist essential top hits"
            )
        }
    }

    private fun buildFavoriteArtists(artists: List<String>): List<DailyMixPlaylist> {
        val seenKeys = mutableSetOf<String>()
        return artists.filter { it.isNotBlank() }
            .filter { artist ->
                val key = canonicalArtistKey(artist)
                key.isNotBlank() && seenKeys.add(key)
            }
            .take(10).mapIndexed { index, artist ->
            val artistId = com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry.registerArtistByName(artist)
            val cover = com.lin0721.linmusic.core.metadata.ArtistPortraitProvider.getCachedPortrait(artist)
            DailyMixPlaylist(
                id = -300L - index,
                title = artist,
                subtitle = "Artist",
                coverUrl = cover,
                isArtistProfile = true,
                artistId = artistId
            )
        }
    }

    private fun buildMoreLikeShelf(
        primaryArtist: String,
        lastFmSimilar: List<String>
    ): List<DailyMixPlaylist> {
        val items = mutableListOf<DailyMixPlaylist>()

        val primaryCover = com.lin0721.linmusic.core.metadata.ArtistPortraitProvider.getCachedPortrait(primaryArtist)

        // 1. Primary Artist Radio
        val related = if (lastFmSimilar.isNotEmpty()) lastFmSimilar.take(6) else listOf("Drake", "Frank Ocean", "Travis Scott", "Kendrick Lamar", "SZA")
        val sideAvatars = related.take(2).map { com.lin0721.linmusic.core.metadata.ArtistPortraitProvider.getCachedPortrait(it) }

        items.add(
            DailyMixPlaylist(
                id = -400L,
                title = primaryArtist,
                subtitle = "$primaryArtist Radio",
                coverUrl = primaryCover,
                coverType = MimikyuCoverType.IMAGE_URL,
                primaryColor = getMimikyuPastelColor(primaryArtist),
                mosaicImages = sideAvatars,
                seedQuery = "$primaryArtist radio"
            )
        )

        // 2. Similar artists from Last.fm
        related.forEachIndexed { idx, relArtist ->
            val relCover = com.lin0721.linmusic.core.metadata.ArtistPortraitProvider.getCachedPortrait(relArtist)
            val artistId = com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry.registerArtistByName(relArtist)
            items.add(
                DailyMixPlaylist(
                    id = -401L - idx,
                    title = relArtist,
                    subtitle = "Artist",
                    coverUrl = relCover,
                    isArtistProfile = true,
                    artistId = artistId
                )
            )
        }

        // 3. Two related genre mixes
        val rAndBCover = com.lin0721.linmusic.core.metadata.ArtistPortraitProvider.getCachedPortrait("SZA")
        val popCover = com.lin0721.linmusic.core.metadata.ArtistPortraitProvider.getCachedPortrait("Taylor Swift")
        items.add(
            DailyMixPlaylist(
                id = -410L,
                title = "R&B Mix",
                subtitle = "$primaryArtist, SZA, Giveon & more",
                coverUrl = rAndBCover,
                coverType = MimikyuCoverType.IMAGE_URL,
                primaryColor = Color(0xFFB490E0),
                seedQuery = "R&B hits"
            )
        )
        items.add(
            DailyMixPlaylist(
                id = -411L,
                title = "Pop Mix",
                subtitle = "$primaryArtist, Lana Del Rey, The Weeknd & more",
                coverUrl = popCover,
                coverType = MimikyuCoverType.IMAGE_URL,
                primaryColor = Color(0xFF7FE4C6),
                seedQuery = "Pop hits"
            )
        )

        return items
    }

    /**
     * Resolves playable tracks on-demand when a playlist is clicked.
     */
    suspend fun getOrGeneratePlaylist(id: Long): DailyMixPlaylist? = withContext(Dispatchers.IO) {
        getCachedMix(id)?.takeIf { it.tracks.isNotEmpty() }?.let { return@withContext it }

        val cached = getCachedMix(id)
        val familiar = tasteSignalScorer?.getFamiliarFavorites()?.map {
            QueueItem(
                songId = it.songId,
                title = it.title,
                artist = it.artist,
                coverUrl = it.coverUrl
            )
        } ?: emptyList()

        val tracks = if (!cached?.seedVideoId.isNullOrBlank()) {
            val candidates = InnerTubeRadioEngine.getRadioCandidates(cached.seedVideoId, count = 35)
            MimikyuPolishFilter.applyPolish(
                novelCandidates = candidates,
                familiarFavorites = familiar,
                targetCount = TRACKS_PER_MIX
            )
        } else {
            val query = cached?.seedQuery?.ifBlank { null } ?: cached?.title ?: "Popular music"
            try {
                val searchRes = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                val songItems = searchRes?.items?.filterIsInstance<com.metrolist.innertube.models.SongItem>() ?: emptyList()

                val raw = songItems.take(TRACKS_PER_MIX + 10).mapNotNull { song ->
                    try {
                        val cleanTitle = cleanMetadataText(song.title)
                        val cleanArtist = cleanMetadataText(song.artists.joinToString(", ") { cleanMetadataText(it.name) })
                        val songId = YouTubeIdRegistry.register(
                            videoId = song.id,
                            title = cleanTitle,
                            artist = cleanArtist,
                            coverUrl = song.thumbnail.orEmpty(),
                            durationSec = song.duration ?: 0
                        )
                        val studioCover = YouTubeIdRegistry.getCoverUrl(songId).takeIf { it.isNotBlank() } ?: song.thumbnail.orEmpty()
                        QueueItem(
                            songId = songId,
                            title = cleanTitle,
                            artist = cleanArtist,
                            coverUrl = studioCover,
                            videoId = song.id,
                            album = cleanMetadataText(song.album?.name.orEmpty()),
                            durationSec = song.duration ?: 0
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                MimikyuPolishFilter.applyPolish(
                    novelCandidates = raw,
                    familiarFavorites = familiar,
                    targetCount = TRACKS_PER_MIX
                )
            } catch (e: Exception) {
                emptyList()
            }
        }

        if (tracks.isEmpty()) return@withContext cached

        val updated = (cached ?: DailyMixPlaylist(
            id = id,
            title = "Personalized Mix",
            subtitle = "Made for you",
            coverUrl = tracks.firstOrNull()?.coverUrl ?: ""
        )).copy(
            tracks = tracks,
            coverUrl = if (cached?.coverUrl?.startsWith("http") == true && !cached.coverUrl.contains("unsplash")) cached.coverUrl else (tracks.firstOrNull()?.coverUrl ?: cached?.coverUrl ?: "")
        )

        cacheMix(updated)
        updated
    }

    private suspend fun fetchUserTopArtists(uid: String): List<Pair<String, String>> {
        val seeds = mutableListOf<Pair<String, String>>()
        val seenArtists = mutableSetOf<String>()

        try {
            val topSongsSnapshot = firestore
                .collection("users")
                .document(uid)
                .collection("topSongs")
                .orderBy("plays", Query.Direction.DESCENDING)
                .limit(TOP_ARTISTS_FETCH.toLong())
                .get()
                .await()

            for (doc in topSongsSnapshot.documents) {
                val videoId = doc.id
                val artist = doc.getString("artist") ?: continue
                val primaryArtist = artist.split(",", "/", "&").firstOrNull()?.trim() ?: artist

                if (seenArtists.add(primaryArtist)) {
                    seeds.add(primaryArtist to videoId)
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to read user topSongs from Firestore: ${e.message}")
        }

        return seeds
    }

    private suspend fun buildMixFromSeed(
        id: Long,
        mixLabel: String,
        subtitleOverride: String? = null,
        seedVideoId: String,
        trackCount: Int,
        coverType: MimikyuCoverType = MimikyuCoverType.DAILY_MIX,
        primaryColor: Color = Color(0xFF1DB954),
        familiarFavorites: List<QueueItem> = emptyList()
    ): DailyMixPlaylist? {
        val radioCandidates = InnerTubeRadioEngine.getRadioCandidates(seedVideoId, count = 35)
        if (radioCandidates.isEmpty()) return null

        val polishedTracks = MimikyuPolishFilter.applyPolish(
            novelCandidates = radioCandidates,
            familiarFavorites = familiarFavorites,
            targetCount = trackCount
        )
        if (polishedTracks.isEmpty()) return null

        val topArtists = polishedTracks
            .map { it.artist.split(",").firstOrNull()?.trim() ?: it.artist }
            .distinct()
            .take(3)
        val defaultSubtitle = when {
            topArtists.size >= 3 -> "${topArtists[0]}, ${topArtists[1]} & more"
            topArtists.size == 2 -> "${topArtists[0]} & ${topArtists[1]}"
            else -> topArtists.firstOrNull() ?: ""
        }

        val coverUrl = polishedTracks.firstOrNull()?.coverUrl ?: ""

        return DailyMixPlaylist(
            id = id,
            title = mixLabel,
            subtitle = subtitleOverride ?: defaultSubtitle,
            coverUrl = coverUrl,
            coverType = coverType,
            primaryColor = primaryColor,
            tracks = polishedTracks,
            seedVideoId = seedVideoId
        )
    }
}
