package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

@Composable
fun PlaylistActionPillsRow(
    isOwnedPlaylist: Boolean,
    isTracksEmpty: Boolean,
    currentSortOption: PlaylistSortOption,
    onAddMusicClick: () -> Unit,
    onEditOrderClick: () -> Unit,
    onSortClick: () -> Unit,
    onEditInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOwnedPlaylist) return

    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = MimikyuSpacing.md)
            .padding(bottom = MimikyuSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(MimikyuSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Add
        ActionPill(
            icon = Icons.Rounded.Add,
            label = "Add",
            enabled = true,
            onClick = onAddMusicClick
        )

        // 2. Edit order
        ActionPill(
            icon = Icons.AutoMirrored.Rounded.Sort,
            label = "Edit",
            enabled = !isTracksEmpty,
            onClick = onEditOrderClick
        )

        // 3. Sort
        ActionPill(
            icon = Icons.Rounded.FilterList,
            label = currentSortOption.label,
            enabled = !isTracksEmpty,
            onClick = onSortClick
        )

        // 4. Playlist info
        ActionPill(
            icon = Icons.Rounded.Edit,
            label = "Info",
            enabled = true,
            onClick = onEditInfoClick
        )
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.4f)
            .pressable(MimikyuPress.Pill, enabled = enabled) { onClick() }
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .animateContentSize(spring(stiffness = Spring.StiffnessMedium))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
