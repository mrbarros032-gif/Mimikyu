package com.lin0721.linmusic.feature.player.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.ui.components.MimikyuIconButton
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

// ：、Songs and Artists、More，Close
@Composable
fun FullScreenLyricsHeader(
    title: String,
    artist: String,
    onClose: () -> Unit,
    onMoreClick: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragRelease: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = MimikyuSpacing.sm, end = 6.dp)
            .padding(vertical = MimikyuSpacing.md)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    onDragDelta(delta)
                },
                onDragStarted = {
                    onDragStart()
                },
                onDragStopped = { velocity ->
                    onDragRelease(velocity)
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MimikyuIconButton(onClick = onClose) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(com.lin0721.linmusic.core.ui.theme.MimikyuIcons.ChevronDown),
                contentDescription = "Collapse lyrics",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        MimikyuIconButton(onClick = onMoreClick) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(com.lin0721.linmusic.core.ui.theme.MimikyuIcons.More),
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
