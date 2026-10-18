package com.lin0721.linmusic.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.LocalBottomOverlayInset
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

@Composable
fun ExtensionsSettingsView(viewModel: SettingsViewModel) {
    val showLockscreen by viewModel.showLockscreen.collectAsStateWithLifecycle()
    val carMode by viewModel.carMode.collectAsStateWithLifecycle()
    val showCreateEntry by viewModel.showCreateEntry.collectAsStateWithLifecycle()
    val deezerArl by viewModel.deezerArl.collectAsStateWithLifecycle()
    val tidalToken by viewModel.tidalToken.collectAsStateWithLifecycle()

    var showDeezerDialog by remember { mutableStateOf(false) }
    var deezerInput by remember(deezerArl) { mutableStateOf(deezerArl) }

    var showTidalDialog by remember { mutableStateOf(false) }
    var tidalInput by remember(tidalToken) { mutableStateOf(tidalToken) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(MimikyuSpacing.md),
            contentPadding = PaddingValues(top = 8.dp, bottom = LocalBottomOverlayInset.current + 16.dp)
        ) {
            item {
                SettingsGroupCard("Lossless Audio Plugins") {
                    SettingsRow(
                        title = "Deezer FLAC Lossless",
                        subtitle = if (deezerArl.isNotBlank()) "Configured (16-bit / 44.1kHz FLAC active)" else "Not configured • Tap to enter ARL token",
                        onClick = { showDeezerDialog = true }
                    )
                    SettingsRow(
                        title = "Tidal Hi-Res Master",
                        subtitle = if (tidalToken.isNotBlank()) "Configured (24-bit / Hi-Res FLAC active)" else "Not configured • Tap to enter Tidal Token",
                        onClick = { showTidalDialog = true }
                    )
                }
            }

            item {
                SettingsGroupCard(SettingsSubMenu.EXTENSIONS.sectionTitles[0]) {
                    SettingsSwitchRow(
                        title = "Enable system lock screen",
                        subtitle = "Show playback controls and lyrics panel on lock screen",
                        checked = showLockscreen,
                        onCheckedChange = { viewModel.updateShowLockscreen(it) }
                    )
                }
            }

            item {
                SettingsGroupCard(SettingsSubMenu.EXTENSIONS.sectionTitles[1]) {
                    SettingsSwitchRow(
                        title = "Car Bluetooth auto resume",
                        subtitle = "Automatically resume playback when connected to car Bluetooth",
                        checked = carMode,
                        onCheckedChange = { viewModel.updateCarMode(it) }
                    )
                }
            }

            item {
                SettingsGroupCard(SettingsSubMenu.EXTENSIONS.sectionTitles[2]) {
                    SettingsSwitchRow(
                        title = "Show create button on bottom bar",
                        subtitle = "Can be created via top-right button in Library when disabled",
                        checked = showCreateEntry,
                        onCheckedChange = { viewModel.updateShowCreateEntry(it) }
                    )
                }
            }
        }

        if (showDeezerDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeezerDialog = false },
                title = { androidx.compose.material3.Text("Deezer ARL Token") },
                text = {
                    androidx.compose.foundation.layout.Column {
                        androidx.compose.material3.Text(
                            "Enter your Deezer 'arl' cookie to enable direct 16-bit / 44.1kHz FLAC Lossless audio downloads.",
                            fontSize = 13.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = deezerInput,
                            onValueChange = { deezerInput = it },
                            label = { androidx.compose.material3.Text("Deezer ARL") },
                            placeholder = { androidx.compose.material3.Text("Paste arl cookie here...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            viewModel.updateDeezerArl(deezerInput.trim())
                            showDeezerDialog = false
                        }
                    ) {
                        androidx.compose.material3.Text("Save")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            viewModel.updateDeezerArl("")
                            deezerInput = ""
                            showDeezerDialog = false
                        }
                    ) {
                        androidx.compose.material3.Text("Clear")
                    }
                }
            )
        }

        if (showTidalDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showTidalDialog = false },
                title = { androidx.compose.material3.Text("Tidal Token") },
                text = {
                    androidx.compose.foundation.layout.Column {
                        androidx.compose.material3.Text(
                            "Enter your Tidal session / API token to enable 24-bit / Hi-Res Master FLAC downloads.",
                            fontSize = 13.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = tidalInput,
                            onValueChange = { tidalInput = it },
                            label = { androidx.compose.material3.Text("Tidal Token") },
                            placeholder = { androidx.compose.material3.Text("Paste Tidal token here...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            viewModel.updateTidalToken(tidalInput.trim())
                            showTidalDialog = false
                        }
                    ) {
                        androidx.compose.material3.Text("Save")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            viewModel.updateTidalToken("")
                            tidalInput = ""
                            showTidalDialog = false
                        }
                    ) {
                        androidx.compose.material3.Text("Clear")
                    }
                }
            )
        }
    }
}

