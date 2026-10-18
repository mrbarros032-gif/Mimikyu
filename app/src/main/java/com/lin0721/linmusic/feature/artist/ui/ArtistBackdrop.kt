package com.lin0721.linmusic.feature.artist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.lin0721.linmusic.core.model.ArtistDetailInfo
import com.lin0721.linmusic.core.ui.components.CoverPlaceholder
import com.lin0721.linmusic.core.ui.theme.extractBaseColorFromUrl

// ：， of ，
@Composable
fun BoxScope.ArtistBackdrop(
    artist: ArtistDetailInfo,
    dominantColor: Color,
    onDominantColorChange: (Color) -> Unit
) {
    val context = LocalContext.current
    val bgUrl = artist.cover.ifEmpty { artist.avatar }

    // ， of
    LaunchedEffect(bgUrl) {
        if (bgUrl.isNotEmpty()) {
            onDominantColorChange(extractBaseColorFromUrl(context, bgUrl))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(if (bgUrl.isNotEmpty()) {
                    if (bgUrl.contains("126.net")) "$bgUrl?param=640y640" else bgUrl
                } else "")
                .allowHardware(false)
                .crossfade(true)
                .build(),
            contentDescription = artist.name,
            contentScale = ContentScale.Crop,
            loading = { CoverPlaceholder() },
            error = { CoverPlaceholder() },
            modifier = Modifier.fillMaxSize()
        )
    }

    // dominantColor ：，， and  Tab
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(390.dp)
            .align(Alignment.TopCenter)
    ) {
        // , and  of  tab_bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(dominantColor)
        )
    }
}
