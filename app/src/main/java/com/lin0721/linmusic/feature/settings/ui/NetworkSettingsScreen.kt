package com.lin0721.linmusic.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.LocalBottomOverlayInset
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

@Composable
fun NetworkSettingsView(viewModel: SettingsViewModel) {
    val wifiOnlyPlay by viewModel.wifiOnlyPlay.collectAsStateWithLifecycle()
    val mobileAlert by viewModel.mobileAlert.collectAsStateWithLifecycle()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(MimikyuSpacing.md),
        contentPadding = PaddingValues(top = 8.dp, bottom = LocalBottomOverlayInset.current + 16.dp)
    ) {
        item {
            SettingsGroupCard(SettingsSubMenu.NETWORK.sectionTitles[0]) {
                SettingsSwitchRow(
                    title = "Stream over Wi-Fi only",
                    subtitle = "When enabled, online tracks cannot be played on mobile data",
                    checked = wifiOnlyPlay,
                    onCheckedChange = { viewModel.updateWifiOnlyPlay(it) }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                SettingsSwitchRow(
                    title = "Cellular playback warning",
                    subtitle = "Show warning when switching from Wi-Fi to mobile data",
                    checked = mobileAlert,
                    onCheckedChange = { viewModel.updateMobileAlert(it) }
                )
            }
        }
    }
}
