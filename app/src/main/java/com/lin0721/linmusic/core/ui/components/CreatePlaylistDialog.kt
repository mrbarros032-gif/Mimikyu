package com.lin0721.linmusic.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

// "Create playlist and save"，Save to Playlist
@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create playlist and save", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Please enter new playlist name:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(MimikyuSpacing.sm))
                PlaceholderTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = "Playlist Name"
                )
            }
        },
        confirmButton = {
            MimikyuTextButton(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onCreate(nameInput)
                        onDismiss()
                    } else {
                        ToastManager.showToast("Name cannot be empty!")
                    }
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Create & Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            MimikyuTextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    )
}
