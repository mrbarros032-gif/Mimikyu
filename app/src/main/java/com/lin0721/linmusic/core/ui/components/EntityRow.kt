package com.lin0721.linmusic.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.lin0721.linmusic.core.ui.theme.RadiusCompact
import com.lin0721.linmusic.core.ui.theme.LocalMimikyuWindowSizeClass
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.MimikyuWindowSizeClass

enum class EntityCoverShape { Rounded, Circle }

// Songs（Albums/Artists/Playlists） of  UI ， SongRow
data class EntityRowData(
    val id: Long,
    val title: String,
    val subtitle: String? = null,
    val coverUrl: String?,
    val coverShape: EntityCoverShape = EntityCoverShape.Rounded
)

// ：Albums/Artists/PlaylistsSongs， SongRow
@Composable
fun EntityRow(
    data: EntityRowData,
    onClick: () -> Unit
) {
    val shape: Shape = when (data.coverShape) {
        EntityCoverShape.Rounded -> RoundedCornerShape(RadiusCompact)
        EntityCoverShape.Circle -> CircleShape
    }
    // Expanded //，， SongRow
    val isExpanded = LocalMimikyuWindowSizeClass.current == MimikyuWindowSizeClass.Expanded
    val coverSize = if (isExpanded) 56.dp else 48.dp
    val verticalPadding = if (isExpanded) 14.dp else 10.dp
    val titleFontSize = if (isExpanded) 17.sp else 15.sp
    val subtitleFontSize = if (isExpanded) 14.sp else 13.sp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MimikyuSpacing.md, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        val imageRequest = remember(data.coverUrl) {
            if (!data.coverUrl.isNullOrBlank()) {
                ImageRequest.Builder(context)
                    .data("${data.coverUrl}?param=100y100")
                    .crossfade(true)
                    .build()
            } else {
                null
            }
        }
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = data.title,
            contentScale = ContentScale.Crop,
            loading = { CoverPlaceholder() },
            error = { CoverPlaceholder() },
            modifier = Modifier.size(coverSize).clip(shape)
        )
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = titleFontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!data.subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = data.subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = subtitleFontSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
