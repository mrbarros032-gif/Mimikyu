package com.lin0721.linmusic.feature.artist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.model.ArtistAlbum
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

// Albums Tab:  of ，
fun LazyListScope.artistAlbumTab(
    albums: List<ArtistAlbum>,
    loadingMore: Boolean,
    onAlbumClick: (Long) -> Unit
) {
    if (albums.isEmpty() && !loadingMore) {
        item(key = "empty_albums") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text("No albums", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    } else {
        items(albums, key = { it.id }) { album ->
            ArtistAlbumRow(album = album, onClick = { onAlbumClick(album.id) })
        }
        if (loadingMore) {
            item(key = "albums_loading_more") {
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
