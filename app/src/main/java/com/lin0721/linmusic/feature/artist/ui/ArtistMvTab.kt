package com.lin0721.linmusic.feature.artist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.model.ArtistMv
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

// MV Tab: ，
fun LazyListScope.artistMvTab(
    mvs: List<ArtistMv>,
    loadingMore: Boolean,
    onMvClick: (ArtistMv) -> Unit
) {
    if (mvs.isEmpty() && !loadingMore) {
        item(key = "empty_mv") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "No MVs",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape)
                        .padding(12.dp)
                )
                Spacer(modifier = Modifier.height(MimikyuSpacing.md))
                Text(
                    text = "No related music videos",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(MimikyuSpacing.xs))
                Text(
                    text = "Artist has not uploaded videos or no playback rights",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    } else {
        items(mvs, key = { it.id }) { mv ->
            ArtistMvCard(mv = mv, onClick = { onMvClick(mv) })
        }
        if (loadingMore) {
            item(key = "mv_loading_more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = MimikyuSpacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}
