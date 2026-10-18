package com.lin0721.linmusic.feature.artist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.components.MimikyuTextButton
import com.lin0721.linmusic.core.ui.components.MimikyuDragHandle
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.model.ArtistDetailInfo

// ────────────────────────────────────────────────────────────────────────────
// Dialog
// ────────────────────────────────────────────────────────────────────────────
@Composable
fun ArtistBioDialog(
    artist: ArtistDetailInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "About ${artist.name}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Box(modifier = Modifier.heightIn(max = 300.dp)) {
                LazyColumn {
                    item {
                        Text(
                            text = artist.briefDesc.trim(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            MimikyuTextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    )
}

// ────────────────────────────────────────────────────────────────────────────
// "Artist Actions" More（/CancelBlock Artist）
// ────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistMoreMenuSheet(
    isBlocked: Boolean,
    onBlockClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = BottomSheetShape,
        dragHandle = { MimikyuDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = MimikyuSpacing.lg, vertical = MimikyuSpacing.md)
        ) {
            Text(
                text = "Artist Options",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = MimikyuSpacing.md)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onBlockClick()
                    }
                    .padding(vertical = MimikyuSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isBlocked) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = if (isBlocked) "Unblock" else "Block Artist",
                    tint = if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(MimikyuSpacing.md))
                Column {
                    Text(
                        text = if (isBlocked) "Unblock all songs from this artist" else "Block all songs from this artist",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isBlocked) "Songs will reappear in recommendations and search once unblocked" else "Songs will no longer appear in recommendations or search once blocked",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
