package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.lin0721.linmusic.core.ui.components.CoverPlaceholder
import com.lin0721.linmusic.core.ui.components.MimikyuButton
import com.lin0721.linmusic.core.ui.components.MimikyuDragHandle
import com.lin0721.linmusic.core.ui.components.MimikyuTextButton
import com.lin0721.linmusic.core.ui.components.PlaceholderTextField
import com.lin0721.linmusic.core.ui.components.ToastManager
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.RadiusCompact

private const val MAX_NAME_LENGTH = 40
private const val MAX_DESC_LENGTH = 1000

// "Edit Playlist Info"，Playlists、 and
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlaylistInfoDialog(
    initialName: String,
    initialDescription: String,
    coverImgUrl: String,
    // Save of ，；Save
    pendingCoverUri: Uri?,
    isSaving: Boolean,
    onEditCoverClick: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nameInput by remember { mutableStateOf(initialName) }
    var descInput by remember { mutableStateOf(initialDescription) }

    val isChanged = nameInput.trim() != initialName.trim() ||
        descInput.trim() != initialDescription.trim() ||
        pendingCoverUri != null
    val canSubmit = nameInput.isNotBlank() && isChanged && !isSaving

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                text = "Edit Playlist Info",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = MimikyuSpacing.md)
            )

            // Playlists：，
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playlist Name",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Text(
                    text = "${nameInput.length}/$MAX_NAME_LENGTH",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(MimikyuSpacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MimikyuSpacing.sm)
            ) {
                val context = LocalContext.current
                // Save of  uri  Coil，
                val coverRequest = remember(coverImgUrl, pendingCoverUri) {
                    ImageRequest.Builder(context)
                        .data(pendingCoverUri ?: "$coverImgUrl?param=100y100")
                        .crossfade(true)
                        .build()
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(RadiusCompact))
                        .clickable(enabled = !isSaving, onClick = onEditCoverClick)
                ) {
                    SubcomposeAsyncImage(
                        model = coverRequest,
                        contentDescription = "Playlist Cover",
                        contentScale = ContentScale.Crop,
                        loading = { CoverPlaceholder() },
                        error = { CoverPlaceholder() },
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Change Cover",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                PlaceholderTextField(
                    value = nameInput,
                    onValueChange = {
                        if (it.length <= MAX_NAME_LENGTH) {
                            nameInput = it
                        }
                    },
                    placeholder = "Enter playlist name",
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(MimikyuSpacing.md))

            // Playlists
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Description",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Text(
                    text = "${descInput.length}/$MAX_DESC_LENGTH",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(MimikyuSpacing.xs))
            PlaceholderTextField(
                value = descInput,
                onValueChange = {
                    if (it.length <= MAX_DESC_LENGTH) {
                        descInput = it
                    }
                },
                placeholder = "Edit description",
                modifier = Modifier.height(100.dp),
                singleLine = false,
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(MimikyuSpacing.lg))


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MimikyuTextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(MimikyuSpacing.sm))
                MimikyuButton(
                    onClick = {
                        val trimmedName = nameInput.trim()
                        if (trimmedName.isEmpty()) {
                            ToastManager.showToast("Playlist name cannot be empty")
                            return@MimikyuButton
                        }
                        if (!isChanged) {
                            onDismiss()
                            return@MimikyuButton
                        }
                        onConfirm(trimmedName, descInput.trim())
                    },
                    enabled = canSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    // ，，Save
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
