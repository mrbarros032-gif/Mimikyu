package com.lin0721.linmusic.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.SurfaceLight

// ：surfaceVariant/surface ，
@Composable
fun Modifier.shimmerBackground(shape: Shape = RectangleShape): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -600f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translateAnim, 0f),
        end = Offset(translateAnim + 400f, 0f)
    )
    return this.background(brush = brush, shape = shape)
}

// /Failed to load of ： + ，。
// AntiFlickerCoverImage ， of  .clip()
@Composable
fun CoverPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceLight),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxSize(0.34f)
        )
    }
}

// of ：，， loading/success ，
// SubcomposeAsyncImage 。 modifier /
@Composable
fun AntiFlickerCoverImage(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onImageLoaded: ((android.graphics.Bitmap) -> Unit)? = null
) {
    val context = LocalContext.current
    var isLoaded by remember(url) { mutableStateOf(false) }
    var isError by remember(url) { mutableStateOf(false) }

    val isGoogleImage = url.contains("=w") || url.contains("=s")
    val fastUrl = remember(url) {
        if (isGoogleImage) url.replace(Regex("=w\\d+-h\\d+.*$"), "=w300-h300-l80-rj") else url
    }
    val highUrl = remember(url) {
        if (isGoogleImage) url.replace(Regex("=w\\d+-h\\d+.*$"), "=w800-h800-l90-rj") else url
    }

    Box(modifier = modifier) {
        if (url.isEmpty() || (!isLoaded && isError)) {
            CoverPlaceholder()
        }

        if (url.isNotEmpty()) {
            // Progressive: bottom layer renders the fast 300x300 thumbnail (~18KB) instantly
            if (fastUrl != highUrl) {
                AsyncImage(
                    model = remember(fastUrl) {
                        ImageRequest.Builder(context)
                            .data(fastUrl)
                            .allowHardware(false)
                            .build()
                    },
                    contentDescription = null,
                    contentScale = contentScale,
                    onState = { state ->
                        if (state is AsyncImagePainter.State.Success) {
                            isLoaded = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top layer crossfades in the crisp 800x800 high-quality art once downloaded
            AsyncImage(
                model = remember(highUrl) {
                    ImageRequest.Builder(context)
                        .data(highUrl)
                        .crossfade(200)
                        .allowHardware(false)
                        .build()
                },
                contentDescription = null,
                contentScale = contentScale,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Success -> {
                            isLoaded = true
                            isError = false
                            val drawable = state.result.drawable
                            val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                                ?: try {
                                    drawable.toBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            if (bitmap != null) {
                                onImageLoaded?.invoke(bitmap)
                            }
                        }
                        is AsyncImagePainter.State.Error -> {
                            if (fastUrl == highUrl) {
                                isLoaded = false
                                isError = true
                            }
                        }
                        else -> Unit
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// EntityRow/SongRow： + ，Search
@Composable
fun SearchResultRowSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MimikyuSpacing.md, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .size(48.dp)
                .shimmerBackground(MaterialTheme.shapes.extraSmall)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Spacer(
                modifier = Modifier
                    .width(160.dp)
                    .height(15.dp)
                    .shimmerBackground(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.height(8.dp))
            Spacer(
                modifier = Modifier
                    .width(100.dp)
                    .height(13.dp)
                    .shimmerBackground(RoundedCornerShape(4.dp))
            )
        }
    }
}

// /Playlists：，
@Composable
fun DiscoverySectionSkeleton() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = MimikyuSpacing.md)) {
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = MimikyuSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(MimikyuSpacing.sm)
            ) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .shimmerBackground(RoundedCornerShape(8.dp))
                )
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .shimmerBackground(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}
