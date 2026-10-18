package com.lin0721.linmusic.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.LocalBottomOverlayInset
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

@Composable
fun ExtensionsSettingsView(viewModel: SettingsViewModel) {
    val showLockscreen by viewModel.showLockscreen.collectAsStateWithLifecycle()
    val carMode by viewModel.carMode.collectAsStateWithLifecycle()
    val showCreateEntry by viewModel.showCreateEntry.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(MimikyuSpacing.md),
            contentPadding = PaddingValues(top = 8.dp, bottom = LocalBottomOverlayInset.current + 16.dp)
        ) {
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
    }
}

