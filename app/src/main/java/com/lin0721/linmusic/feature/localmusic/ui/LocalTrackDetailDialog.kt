package com.lin0721.linmusic.feature.localmusic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.localmusic.LocalTrack
import com.lin0721.linmusic.core.localmusic.LocalTrackSource
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.common.formatFileSize

private fun formatTrackDuration(durationMs: Long): String {
    if (durationMs <= 0) return "--:--"
    val totalSeconds = durationMs / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
fun LocalTrackDetailDialog(track: LocalTrack, onDismiss: () -> Unit) {
    val extension = track.path?.substringAfterLast('.', "")?.uppercase()?.takeIf { it.isNotBlank() }
        ?: track.uri.toString().substringAfterLast('.', "").uppercase().takeIf { it.isNotBlank() }
        ?: "Unknown"
    val sourceLabel = if (track.source == LocalTrackSource.MIMIKYU_DOWNLOAD) "Mimikyu Downloads" else "Device Local File"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(track.title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MimikyuSpacing.sm)) {
                DetailRow("Artist", track.artist)
                DetailRow("Album", track.album?.takeIf { it.isNotBlank() } ?: "Unknown Album")
                DetailRow("Format", extension)
                DetailRow("Duration", formatTrackDuration(track.durationMs))
                DetailRow("Size", formatFileSize(track.sizeBytes))
                DetailRow("Source", sourceLabel)
                if (track.path != null) {
                    DetailRow("Path", track.path)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(start = MimikyuSpacing.md)
        )
    }
}
