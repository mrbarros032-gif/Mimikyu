package com.lin0721.linmusic.feature.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.components.MimikyuTextButton
import com.lin0721.linmusic.core.ui.components.MimikyuButton
import com.lin0721.linmusic.core.ui.components.MimikyuDragHandle
import com.lin0721.linmusic.core.ui.components.ToastManager
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

// ────────────────────────────────────────────────────────────────────────────
// "New Playlist"
// ────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryCreatePlaylistSheet(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var playlistNameInput by remember { mutableStateOf("") }
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
                .imePadding()
                .padding(horizontal = MimikyuSpacing.lg, vertical = MimikyuSpacing.md)
        ) {
            Text(
                text = "New Playlist",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = MimikyuSpacing.md)
            )
            Text(
                text = "Please enter new playlist name:",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = MimikyuSpacing.md),
                contentAlignment = Alignment.CenterStart
            ) {
                if (playlistNameInput.isEmpty()) {
                    Text("Playlist Name", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                BasicTextField(
                    value = playlistNameInput,
                    onValueChange = { playlistNameInput = it },
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(MimikyuSpacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MimikyuTextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(MimikyuSpacing.md))
                MimikyuButton(
                    onClick = {
                        if (playlistNameInput.isNotBlank()) {
                            onCreate(playlistNameInput)
                        } else {
                            ToastManager.showToast("Name cannot be empty!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// "Sort By"
// ────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySortMenuSheet(
    currentSortOrder: LibrarySortOrder,
    onSelect: (LibrarySortOrder) -> Unit,
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
                text = "Sort By",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = MimikyuSpacing.md)
            )

            val sortOptions = listOf(
                LibrarySortOrder.RECENTLY_PLAYED to "Recently Played",
                LibrarySortOrder.NAME to "Alphabetical",
                LibrarySortOrder.CUSTOM to "Custom"
            )

            sortOptions.forEach { (order, label) ->
                val isSelected = currentSortOrder == order
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(order)
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
