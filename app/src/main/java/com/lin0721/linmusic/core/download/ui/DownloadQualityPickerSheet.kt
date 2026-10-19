package com.lin0721.linmusic.core.download.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.core.download.DownloadPreferences
import com.lin0721.linmusic.core.preferences.SettingsPreferences
import com.lin0721.linmusic.core.ui.components.ToastManager
import com.lin0721.linmusic.core.ui.theme.BackgroundDark
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.ui.theme.DownloadedGreen
import com.lin0721.linmusic.core.ui.theme.DragHandleShape
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import org.koin.compose.koinInject

data class DownloadQualityOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val isAvailable: Boolean,
    val missingHint: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQualityPickerSheet(
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    songId: Long? = null,
    maxDownloadLevel: String? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsPreferences: SettingsPreferences = koinInject()
    val deezerArl by settingsPreferences.deezerArl.collectAsStateWithLifecycle(initialValue = "")
    val tidalToken by settingsPreferences.tidalToken.collectAsStateWithLifecycle(initialValue = "")

    val downloadedQuality: String? = if (songId != null) {
        val downloadPreferences: DownloadPreferences = koinInject()
        val quality by downloadPreferences.downloadedQualityFor(songId).collectAsStateWithLifecycle(initialValue = null)
        quality
    } else {
        null
    }

    val options = listOf(
        DownloadQualityOption(
            id = "standard",
            title = "Standard Quality",
            subtitle = "YouTube AAC • 128 kbps",
            isAvailable = true
        ),
        DownloadQualityOption(
            id = "high",
            title = "High Quality",
            subtitle = "YouTube Opus • 256 kbps",
            isAvailable = true
        ),
        DownloadQualityOption(
            id = "lossless",
            title = "Lossless FLAC",
            subtitle = "Deezer • 16-bit / 44.1kHz FLAC",
            isAvailable = deezerArl.isNotBlank(),
            missingHint = "Requires Deezer ARL in Settings > Extensions"
        ),
        DownloadQualityOption(
            id = "master",
            title = "Master Hi-Res",
            subtitle = "Tidal • 24-bit / Hi-Res FLAC",
            isAvailable = tidalToken.isNotBlank(),
            missingHint = "Requires Tidal Token in Settings > Extensions"
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BackgroundDark,
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
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = MimikyuSpacing.lg, end = MimikyuSpacing.lg, bottom = MimikyuSpacing.lg)
        ) {
            Text(
                text = "Select Download Quality",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = MimikyuSpacing.xs)
            )
            Text(
                text = "Audio will be saved with synced TTML & LRC lyrics",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = MimikyuSpacing.md)
            )

            options.forEach { option ->
                val isSelected = option.id == downloadedQuality
                val clickableModifier = if (option.isAvailable) {
                    Modifier.clickable { onQualitySelected(option.id) }
                } else {
                    Modifier.clickable {
                        ToastManager.showToast(option.missingHint ?: "Plugin credential required")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(clickableModifier)
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .alpha(if (option.isAvailable) 1f else 0.38f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = option.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (!option.isAvailable) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Plugin Required",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (option.isAvailable) option.subtitle else (option.missingHint ?: option.subtitle),
                            color = if (option.isAvailable) Color.White.copy(alpha = 0.6f) else Color(0xFFFF8A80),
                            fontSize = 12.sp
                        )
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, DownloadedGreen, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Downloaded",
                                color = DownloadedGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
