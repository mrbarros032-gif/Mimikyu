package com.lin0721.linmusic.core.metadata

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * MetadataEnricher:
 * Layered album art correction + upscaling pipeline to fix wrong and low-res album art:
 *
 * Resolution order per song (tries each until one succeeds):
 * 1. iTunes Search API (https://itunes.apple.com/search) — match on title+artist, use
 *    artworkUrl100 upgraded to 1200x1200. Clean commercial art (primary).
 * 2. FALLBACK: MusicBrainz + Cover Art Archive (open source, no API key) —
 *    Query MusicBrainz to identify the release (title+artist fuzzy match), then fetch front
 *    cover art from Cover Art Archive (up to 1200x1200). Respects 1 req/sec rate limit & User-Agent.
 * 3. LAST RESORT: YouTube's own thumbnail, upscaled to 1200x1200 via URL parameter
 *    (e.g. =w1200-h1200-l90-rj or maxresdefault.jpg).
 *
 * All inputs are cleaned with [cleanMetadataText] before matching to maximize accuracy.
 * Caches the winning result per song in local persistent storage so resolution only runs once per song.
 */
class MetadataEnricher(
    private val context: Context,
    private val client: ITunesSearchClient = ITunesSearchClient(),
    private val musicBrainzClient: MusicBrainzClient = MusicBrainzClient(),
    private val cache: MetadataCache = MetadataCache(context),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val inFlightMutex = Mutex()
    private val inFlight = ConcurrentHashMap<String, kotlinx.coroutines.Deferred<EnrichedMetadata>>()

    private val _metadataUpdates = MutableStateFlow<Map<String, EnrichedMetadata>>(emptyMap())
    val metadataUpdates: StateFlow<Map<String, EnrichedMetadata>> = _metadataUpdates.asStateFlow()

    companion object {
        private const val TAG = "MetadataEnricher"
    }

    /**
     * Synchronously returns cached metadata if available, otherwise null.
     */
    fun getCached(songId: String): EnrichedMetadata? {
        return cache.get(songId)
    }

    /**
     * Synchronously returns cached metadata by title and artist query if available.
     */
    fun getCachedByQuery(title: String, artist: String): EnrichedMetadata? {
        val (cleanTitle, cleanArtist) = extractCleanTitleAndArtist(title, artist)
        return cache.getByQuery(cleanTitle, cleanArtist)
    }

    /**
     * Resolves enriched metadata for a track.
     * If cached, returns immediately without network request.
     * Deduplicates concurrent requests for the same songId or title/artist.
     */
    suspend fun resolve(
        songId: String,
        title: String,
        artist: String,
        album: String? = null,
        fallbackCoverUrl: String? = null,
        durationMs: Long? = null
    ): EnrichedMetadata {
        val (cleanTitle, cleanArtist) = extractCleanTitleAndArtist(title, artist)
        val cleanAlbum = album?.let { cleanMetadataText(it) }

        if (songId.isBlank() && cleanTitle.isBlank()) {
            val upscaled = YouTubeThumbnailUpscaler.upscale(fallbackCoverUrl) ?: fallbackCoverUrl
            return EnrichedMetadata(
                songId = songId,
                title = cleanTitle,
                artist = cleanArtist,
                albumName = cleanAlbum,
                artworkUrl = upscaled,
                durationMs = durationMs,
                isEnriched = false,
                artSource = if (upscaled != null) "youtube" else "none"
            )
        }

        // 1. Check local cache (by songId first, then by title:artist query)
        val cached = if (songId.isNotBlank()) cache.get(songId) else null
        if (cached != null) {
            saveResult(cached)
            Timber.tag(TAG).d("Cache hit for songId=$songId source=${cached.artSource} art=${cached.artworkUrl}")
            return cached
        }

        val cachedByQuery = if (cleanTitle.isNotBlank() && cleanArtist.isNotBlank()) {
            cache.getByQuery(cleanTitle, cleanArtist)
        } else null
        if (cachedByQuery != null) {
            val combined = if (songId.isNotBlank()) cachedByQuery.copy(songId = songId) else cachedByQuery
            saveResult(combined)
            Timber.tag(TAG).d("Query cache hit for '$cleanTitle' by '$cleanArtist' source=${combined.artSource}")
            return combined
        }

        // 2. Deduplicate in-flight requests
        val dedupeKey = if (songId.isNotBlank()) songId else "$cleanTitle:$cleanArtist"
        val deferred = inFlightMutex.withLock {
            inFlight.getOrPut(dedupeKey) {
                scope.async {
                    performEnrichment(songId, cleanTitle, cleanArtist, cleanAlbum, fallbackCoverUrl, durationMs)
                }
            }
        }

        val result = try {
            deferred.await()
        } finally {
            inFlight.remove(dedupeKey)
        }

        return result
    }

    /**
     * Trigger background enrichment non-blockingly.
     * Call this when a song is loaded, played, added to playlist, or downloaded.
     */
    fun enrichInBackground(
        songId: String,
        title: String,
        artist: String,
        album: String? = null,
        fallbackCoverUrl: String? = null,
        durationMs: Long? = null,
        onComplete: ((EnrichedMetadata) -> Unit)? = null
    ): Job {
        return scope.launch {
            try {
                val enriched = resolve(songId, title, artist, album, fallbackCoverUrl, durationMs)
                _metadataUpdates.update { current ->
                    current + (songId to enriched)
                }
                onComplete?.invoke(enriched)
            } catch (e: Exception) {
                Timber.tag(TAG).w("Background enrichment failed for $songId: ${e.message}")
            }
        }
    }

    /**
     * Layered resolution pipeline:
     * 1. iTunes Search API (1200x1200bb verified commercial art)
     * 2. MusicBrainz + Cover Art Archive (verified front cover art + verified metadata)
     * 3. YouTube thumbnail upscaled via URL parameter (last resort)
     */
    private suspend fun performEnrichment(
        songId: String,
        cleanTitle: String,
        cleanArtist: String,
        cleanAlbum: String?,
        fallbackCoverUrl: String?,
        durationMs: Long?
    ): EnrichedMetadata {
        Timber.tag(TAG).i("Starting art resolution chain for '$cleanTitle' - '$cleanArtist' (songId=$songId)")

        // -------------------------------------------------------------
        // LAYER 1: Laya Decision Engine — iTunes vs YouTube Evaluation
        // -------------------------------------------------------------
        val itunesEnriched = tryResolveITunes(songId, cleanTitle, cleanArtist, cleanAlbum, durationMs)
        val itunesArt = itunesEnriched?.artworkUrl
        val ytArt = YouTubeThumbnailUpscaler.upscale(fallbackCoverUrl) ?: fallbackCoverUrl

        val coverDecision = com.lin0721.linmusic.core.laya.LayaDecisionEngine.chooseBestCover(
            title = cleanTitle,
            artist = cleanArtist,
            ytCoverUrl = ytArt,
            itunesCoverUrl = itunesArt
        )

        if (coverDecision.selectedUrl.isNotBlank()) {
            val resolvedEnriched = EnrichedMetadata(
                songId = songId,
                title = itunesEnriched?.title ?: cleanTitle,
                artist = itunesEnriched?.artist ?: cleanArtist,
                albumName = itunesEnriched?.albumName ?: cleanAlbum,
                artworkUrl = coverDecision.selectedUrl,
                releaseDate = itunesEnriched?.releaseDate,
                genre = itunesEnriched?.genre,
                durationMs = durationMs,
                isEnriched = true,
                artSource = coverDecision.source
            )
            Timber.tag(TAG).i("Laya cover decision (${coverDecision.source}, matched=${coverDecision.matched}) for '$cleanTitle': ${coverDecision.selectedUrl} - ${coverDecision.rationale}")
            saveResult(resolvedEnriched)
            return resolvedEnriched
        }

        // -------------------------------------------------------------
        // LAYER 2: Fallback — MusicBrainz + Cover Art Archive
        // -------------------------------------------------------------
        val mbMatch = tryResolveMusicBrainz(cleanTitle, cleanArtist, cleanAlbum, durationMs)
        if (mbMatch != null) {
            val verifiedArt = mbMatch.artworkUrl.takeIf { !it.isNullOrBlank() }
                ?: (YouTubeThumbnailUpscaler.upscale(fallbackCoverUrl) ?: fallbackCoverUrl)
            val artSource = if (!mbMatch.artworkUrl.isNullOrBlank()) "musicbrainz" else "youtube"

            val mbEnriched = EnrichedMetadata(
                songId = songId,
                title = mbMatch.title.ifBlank { itunesEnriched?.title ?: cleanTitle },
                artist = mbMatch.artist.ifBlank { itunesEnriched?.artist ?: cleanArtist },
                albumName = mbMatch.album?.ifBlank { itunesEnriched?.albumName ?: cleanAlbum } ?: (itunesEnriched?.albumName ?: cleanAlbum),
                artworkUrl = verifiedArt,
                releaseDate = mbMatch.releaseDate ?: itunesEnriched?.releaseDate,
                genre = itunesEnriched?.genre,
                durationMs = durationMs,
                isEnriched = true,
                artSource = artSource
            )
            Timber.tag(TAG).i("Layer 2 SUCCESS (MusicBrainz / CAA) for '$cleanTitle': $verifiedArt")
            saveResult(mbEnriched)
            return mbEnriched
        }

        // -------------------------------------------------------------
        // LAYER 3: Last Resort — YouTube's own thumbnail upscaled
        // -------------------------------------------------------------
        val upscaledYouTubeArt = YouTubeThumbnailUpscaler.upscale(fallbackCoverUrl)
        val finalArtwork = upscaledYouTubeArt ?: fallbackCoverUrl
        val artSource = if (!upscaledYouTubeArt.isNullOrBlank()) "youtube" else "none"

        val fallback = EnrichedMetadata(
            songId = songId,
            title = itunesEnriched?.title ?: cleanTitle,
            artist = itunesEnriched?.artist ?: cleanArtist,
            albumName = itunesEnriched?.albumName ?: cleanAlbum,
            artworkUrl = finalArtwork,
            releaseDate = itunesEnriched?.releaseDate,
            genre = itunesEnriched?.genre,
            durationMs = durationMs,
            isEnriched = false,
            artSource = artSource
        )
        Timber.tag(TAG).i("Layer 3 LAST RESORT (YouTube upscaled) for '$cleanTitle' ($artSource): $finalArtwork")
        saveResult(fallback)
        return fallback
    }

    private suspend fun tryResolveITunes(
        songId: String,
        title: String,
        artist: String,
        album: String?,
        durationMs: Long?
    ): EnrichedMetadata? {
        Timber.tag(TAG).d("Querying iTunes for '$title' by '$artist'")

        return try {
            val results = client.searchSongsByTitleAndArtist(title, artist, limit = 5)
            var bestTrack: ITunesTrack? = null
            var highestConfidence = 0.0

            for (candidate in results) {
                val confidence = FuzzyMatcher.matchConfidence(
                    queryTitle = title,
                    queryArtist = artist,
                    candidateTitle = cleanMetadataText(candidate.trackName),
                    candidateArtist = cleanMetadataText(candidate.artistName),
                    queryAlbum = album,
                    candidateAlbum = candidate.collectionName?.let { cleanMetadataText(it) },
                    queryDurationMs = durationMs,
                    candidateDurationMs = candidate.trackTimeMillis
                )

                if (confidence > highestConfidence) {
                    highestConfidence = confidence
                    bestTrack = candidate
                }
            }

            if (bestTrack != null && FuzzyMatcher.isConfidentMatch(highestConfidence)) {
                val upgradedArtwork = bestTrack.getUpgradedArtworkUrl(1200)
                EnrichedMetadata(
                    songId = songId,
                    title = cleanMetadataText(bestTrack.trackName).ifBlank { title },
                    artist = cleanMetadataText(bestTrack.artistName).ifBlank { artist },
                    albumName = bestTrack.collectionName?.let { cleanMetadataText(it) } ?: album,
                    artworkUrl = upgradedArtwork,
                    releaseDate = bestTrack.releaseDate,
                    genre = bestTrack.primaryGenreName,
                    durationMs = bestTrack.trackTimeMillis ?: durationMs,
                    isEnriched = true,
                    artSource = "itunes"
                )
            } else {
                Timber.tag(TAG).d("No confident iTunes match for '$title' by '$artist' (highestConfidence=$highestConfidence)")
                null
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w("iTunes query error for '$title' by '$artist': ${e.message}")
            null
        }
    }

    private suspend fun tryResolveMusicBrainz(
        title: String,
        artist: String,
        album: String?,
        durationMs: Long?
    ): MusicBrainzMatch? {
        Timber.tag(TAG).d("Querying MusicBrainz for '$title' by '$artist'")
        return try {
            musicBrainzClient.resolveVerifiedTrack(
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs
            )
        } catch (e: Exception) {
            Timber.tag(TAG).w("MusicBrainz lookup failed for '$title': ${e.message}")
            null
        }
    }

    private suspend fun saveResult(metadata: EnrichedMetadata) {
        cache.put(metadata)
        val idLong = metadata.songId.toLongOrNull()
        if (idLong != null) {
            YouTubeIdRegistry.registerEnrichedMetadata(
                songId = idLong,
                title = metadata.title,
                artist = metadata.artist,
                album = metadata.albumName ?: "",
                releaseDate = metadata.releaseDate ?: "",
                genre = metadata.genre ?: "",
                coverUrl = metadata.artworkUrl ?: ""
            )
        }
    }

}
