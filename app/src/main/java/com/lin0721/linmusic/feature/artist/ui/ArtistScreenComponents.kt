package com.lin0721.linmusic.feature.artist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.lin0721.linmusic.core.ui.components.CoverPlaceholder
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.RadiusCompact
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.model.ArtistAlbum
import com.lin0721.linmusic.core.model.ArtistInfo
import com.lin0721.linmusic.core.model.ArtistMv
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SimilarArtistCard(
    artist: ArtistInfo,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .pressable(MimikyuPress.Card, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = if (artist.avatarUrl.contains("126.net")) "${artist.avatarUrl}?param=150y150" else artist.avatarUrl,
            contentDescription = artist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
        )

        Spacer(Modifier.height(MimikyuSpacing.sm))

        Text(
            text = artist.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = MimikyuSpacing.xs)
        )
    }
}

@Composable
fun ArtistAlbumRow(
    album: ArtistAlbum,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = MimikyuSpacing.md, vertical = MimikyuSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SubcomposeAsyncImage(
            model = "${album.picUrl}?param=150y150",
            contentDescription = album.name,
            contentScale = ContentScale.Crop,
            loading = { CoverPlaceholder() },
            error = { CoverPlaceholder() },
            modifier = Modifier
                .size(64.dp)
                .clip(MaterialTheme.shapes.small)
        )
        Spacer(modifier = Modifier.width(MimikyuSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            val publishTimeStr = remember(album.publishTime) {
                com.lin0721.linmusic.core.common.formatAlbumReleaseDate(album.publishTime)
            }
            val desc = buildString {
                if (publishTimeStr.isNotEmpty()) append(publishTimeStr)
                if (album.size > 0) {
                    if (isNotEmpty()) append(" • ")
                    append("${album.size} songs")
                }
            }
            Text(
                text = desc,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "Details",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

fun formatFansCount(count: Long): String {
    return when {
        count >= 1_000_000 -> {
            val millions = count / 1_000_000.0
            val formatted = String.format(Locale.US, "%.1f", millions)
            if (formatted.endsWith(".0")) formatted.substring(0, formatted.length - 2) + "M" else formatted + "M"
        }
        count >= 1_000 -> {
            val thousands = count / 1_000.0
            val formatted = String.format(Locale.US, "%.1f", thousands)
            if (formatted.endsWith(".0")) formatted.substring(0, formatted.length - 2) + "K" else formatted + "K"
        }
        else -> NumberFormat.getNumberInstance(Locale.US).format(count)
    }
}

fun formatExactCount(count: Long): String {
    return NumberFormat.getNumberInstance(Locale.US).format(count)
}

// mm:ss ， MV
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun ArtistMvCard(
    mv: ArtistMv,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Column(
        modifier = modifier
            .pressable(MimikyuPress.Card, onClick = onClick)
            .padding(horizontal = MimikyuSpacing.md, vertical = MimikyuSpacing.sm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(RadiusCompact))
        ) {
            SubcomposeAsyncImage(
                model = "${mv.cover}?param=500y280",
                contentDescription = mv.name,
                contentScale = ContentScale.Crop,
                loading = { CoverPlaceholder() },
                error = { CoverPlaceholder() },
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            if (mv.duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = formatDuration(mv.duration), color = Color.White, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(MimikyuSpacing.sm))
        Text(
            text = mv.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (mv.playCount > 0) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${formatFansCount(mv.playCount)} views",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}
