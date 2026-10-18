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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.core.download.DownloadPreferences
import com.lin0721.linmusic.core.model.getQualityDisplayName
import com.lin0721.linmusic.core.ui.theme.BackgroundDark
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.ui.theme.DownloadedGreen
import com.lin0721.linmusic.core.ui.theme.DragHandleShape
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import org.koin.compose.koinInject

// Download
private val DOWNLOAD_QUALITIES = listOf(
    "standard", "higher", "exhigh", "lossless", "hires", "jyeffect", "sky", "jymaster"
)

// Download
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQualityPickerSheet(
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    songId: Long? = null,
    maxDownloadLevel: String? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val downloadedQuality: String? = if (songId != null) {
        val downloadPreferences: DownloadPreferences = koinInject()
        val quality by downloadPreferences.downloadedQualityFor(songId).collectAsStateWithLifecycle(initialValue = null)
        quality
    } else {
        null
    }

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
                modifier = Modifier.padding(bottom = MimikyuSpacing.md)
            )
            DOWNLOAD_QUALITIES.forEach { quality ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQualitySelected(quality) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = getQualityDisplayName(quality),
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (quality == downloadedQuality) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, DownloadedGreen, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Downloaded",
                                color = DownloadedGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
