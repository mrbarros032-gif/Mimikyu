package com.lin0721.linmusic.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.lin0721.linmusic.core.metadata.MetadataEnricher
import com.lin0721.linmusic.core.metadata.ThumbnailUpscaleInterceptor
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import com.lin0721.linmusic.core.ui.theme.RadiusCompact
import org.koin.java.KoinJavaComponent

/**
 * Unified, reusable album artwork component for Mimikyu.
 *
 * Ensures consistent high-resolution image rendering across Home grid, Search,
 * Playlists, Library, and Player screens:
 * 1. Checks YouTubeIdRegistry for high-res / enriched studio artwork (iTunes / Deezer / CAA / Spotify).
 * 2. Asynchronously enriches from MetadataEnricher if current art is a YouTube thumbnail.
 * 3. Automatically upscales YouTube Music thumbnails to 1200x1200 with square crop.
 * 4. Handles placeholders and error states gracefully.
 */
@Composable
fun AlbumArtImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    songId: Long? = null,
    title: String? = null,
    artist: String? = null,
    shape: Shape = RoundedCornerShape(RadiusCompact),
    contentScale: ContentScale = ContentScale.Crop,
    loading: @Composable () -> Unit = { CoverPlaceholder() },
    error: @Composable () -> Unit = { CoverPlaceholder() }
) {
    val context = LocalContext.current
    val enricher = remember {
        runCatching { KoinJavaComponent.getKoin().getOrNull<MetadataEnricher>() }.getOrNull()
    }

    // Determine initial best art candidate
    val initialBest = remember(url, songId, title, artist) {
        val regCover = songId?.let { YouTubeIdRegistry.getCoverUrl(it) }?.takeIf { it.isNotBlank() }
        if (YouTubeIdRegistry.isStudioCover(regCover)) {
            regCover!!
        } else if (YouTubeIdRegistry.isStudioCover(url)) {
            url!!
        } else {
            val cached = songId?.let { enricher?.getCached(it.toString()) }
                ?: if (!title.isNullOrBlank() && !artist.isNullOrBlank()) enricher?.getCachedByQuery(title, artist) else null
            cached?.artworkUrl ?: regCover ?: url.orEmpty()
        }
    }

    var effectiveArt by remember(url, songId, title, artist) {
        mutableStateOf(initialBest)
    }

    // If not a studio cover yet, asynchronously query MetadataEnricher
    LaunchedEffect(url, songId, title, artist) {
        if (!YouTubeIdRegistry.isStudioCover(effectiveArt) && !title.isNullOrBlank() && !artist.isNullOrBlank()) {
            val res = enricher?.resolve(
                songId = songId?.toString().orEmpty(),
                title = title,
                artist = artist,
                fallbackCoverUrl = url
            )
            if (res != null && res.isEnriched && !res.artworkUrl.isNullOrBlank()) {
                effectiveArt = res.artworkUrl
            }
        }
    }

    val finalResolvedUrl = remember(effectiveArt) {
        ThumbnailUpscaleInterceptor.resolveUrl(effectiveArt)
    }

    val request = remember(finalResolvedUrl) {
        if (finalResolvedUrl.isNotBlank()) {
            ImageRequest.Builder(context)
                .data(finalResolvedUrl)
                .crossfade(true)
                .build()
        } else null
    }

    Box(modifier = modifier.clip(shape)) {
        if (request != null) {
            SubcomposeAsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = contentScale,
                loading = { loading() },
                error = { error() },
                modifier = Modifier.matchParentSize()
            )
        } else {
            loading()
        }
    }
}
