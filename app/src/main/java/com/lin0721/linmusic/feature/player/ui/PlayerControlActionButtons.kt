package com.lin0721.linmusic.feature.player.ui

import android.media.AudioDeviceInfo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.components.MimikyuIconButton
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.InfoCardRadius
import com.lin0721.linmusic.core.ui.theme.TextGray
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

// ：、Share、PlayQueue
// connectedDevice Current（/），、，；Play
@Composable
fun ActionButtons(
    onOutputDeviceClick: () -> Unit,
    onQueueClick: () -> Unit,
    onShareClick: () -> Unit,
    connectedDevice: AudioDeviceInfo? = null
) {
    val outputDeviceIcon = connectedDevice?.let { deviceIcon(it.type) } ?: Icons.Rounded.SpeakerGroup
    val outputDeviceTint = if (connectedDevice != null) MaterialTheme.colorScheme.primary else TextGray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MimikyuSpacing.lg)
            .padding(top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f, fill = false)
                .pressable(MimikyuPress.None, onClick = onOutputDeviceClick)
        ) {
            MimikyuIconButton(
                onClick = onOutputDeviceClick,
                modifier = Modifier.size(28.dp)
            ) {
                if (connectedDevice != null) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(deviceIcon(connectedDevice.type)),
                        contentDescription = "Connect device",
                        tint = outputDeviceTint,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(com.lin0721.linmusic.core.ui.theme.MimikyuIcons.Devices),
                        contentDescription = "Connect device",
                        tint = outputDeviceTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (connectedDevice != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = deviceLabel(connectedDevice),
                    color = outputDeviceTint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 160.dp)
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MimikyuIconButton(
                onClick = onShareClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(com.lin0721.linmusic.core.ui.theme.MimikyuIcons.Share),
                    contentDescription = "Share",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
            MimikyuIconButton(
                onClick = onQueueClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(com.lin0721.linmusic.core.ui.theme.MimikyuIcons.Queue),
                    contentDescription = "Queue",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
