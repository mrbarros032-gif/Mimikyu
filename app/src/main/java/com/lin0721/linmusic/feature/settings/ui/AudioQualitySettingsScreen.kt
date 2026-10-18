package com.lin0721.linmusic.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.LocalBottomOverlayInset
import com.lin0721.linmusic.core.model.getQualityDisplayName
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.ui.theme.DragHandleShape
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioQualitySettingsView(viewModel: SettingsViewModel) {
    val wifiQuality by viewModel.wifiQuality.collectAsStateWithLifecycle()
    val mobileQuality by viewModel.mobileQuality.collectAsStateWithLifecycle()

    var qualityDialogTarget by remember { mutableStateOf<String?>(null) }

    // of
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(MimikyuSpacing.md),
        contentPadding = PaddingValues(top = 8.dp, bottom = LocalBottomOverlayInset.current + 16.dp)
    ) {
        item {
            SettingsGroupCard(SettingsSubMenu.AUDIO_QUALITY.sectionTitles[0]) {
                SettingsRow(
                    title = "Wi-Fi Sound Profile",
                    subtitle = getQualityDisplayName(wifiQuality),
                    onClick = { qualityDialogTarget = "wifi" }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                SettingsRow(
                    title = "Cellular Sound Profile",
                    subtitle = getQualityDisplayName(mobileQuality),
                    onClick = { qualityDialogTarget = "mobile" }
                )
            }
        }

        item {
            SettingsGroupCard("Ultra HD DSP Engine") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Lossless-Beater Harman Curve",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Powered by Android 32-bit hardware DynamicsProcessing. Includes subtractive 300Hz mud cut, +3.2dB 16kHz air boost, multi-band compression, brickwall limiter, and 25% stereo soundstage widener for studio monitor sound on all headphones.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }


    val qualityTarget = qualityDialogTarget
    if (qualityTarget != null) {
        val presets = listOf(
            com.lin0721.linmusic.core.player.AudioMasteringPreset.STUDIO_MASTER,
            com.lin0721.linmusic.core.player.AudioMasteringPreset.BASS_BOOST,
            com.lin0721.linmusic.core.player.AudioMasteringPreset.VOCAL_ACOUSTIC,
            com.lin0721.linmusic.core.player.AudioMasteringPreset.FLAT
        )
        val isWifi = qualityTarget == "wifi"
        val activeQuality = if (isWifi) wifiQuality else mobileQuality
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { qualityDialogTarget = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            shape = BottomSheetShape,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = MimikyuSpacing.xs)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(DragHandleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = MimikyuSpacing.lg, end = MimikyuSpacing.lg, bottom = MimikyuSpacing.lg)
            ) {
                Text(
                    text = if (isWifi) "Select Wi-Fi Sound Profile" else "Select Cellular Sound Profile",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Hardware DSP 8-band parametric curve & multi-band compressor",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = MimikyuSpacing.md)
                )

                presets.forEach { preset ->
                    val isSelected = activeQuality == preset.key ||
                            (preset == com.lin0721.linmusic.core.player.AudioMasteringPreset.STUDIO_MASTER &&
                                    (activeQuality == "lossless" || activeQuality == "standard" || activeQuality == "exhigh" || activeQuality == "hires" || activeQuality == "jymaster"))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable {
                                if (isWifi) {
                                    viewModel.updateWifiQuality(preset.key)
                                } else {
                                    viewModel.updateMobileQuality(preset.key)
                                }
                                com.lin0721.linmusic.core.player.UltraAudioMasteringEngine.setPreset(preset)
                                qualityDialogTarget = null
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = preset.title,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = preset.badge,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = preset.subtitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
