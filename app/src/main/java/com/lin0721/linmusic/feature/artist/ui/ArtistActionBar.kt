package com.lin0721.linmusic.feature.artist.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lin0721.linmusic.core.ui.components.MimikyuIconButton
import com.lin0721.linmusic.core.model.ArtistDetailInfo
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.interaction.pressScale
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.PillRadius

// ：Followers、Follow、More and /AllPlay
@Composable
fun ArtistActionBar(
    artist: ArtistDetailInfo,
    fansCount: Long,
    mimikyuMonthlyListeners: Long = 0L,
    isFollowed: Boolean,
    onFollowClick: () -> Unit,
    onMoreClick: () -> Unit,
    onPlayAll: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp) // 80.dp
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.60f), // ：80%
                        MaterialTheme.colorScheme.background.copy(alpha = 1.00f)  // ：90%
                    )
                )
            )
            .padding(start = MimikyuSpacing.md, end = MimikyuSpacing.md, top = MimikyuSpacing.xs, bottom = MimikyuSpacing.xs),
        verticalAlignment = Alignment.Bottom
    ) {
        // (Followers and )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            val listenerText = when {
                mimikyuMonthlyListeners > 0L -> "${formatFansCount(mimikyuMonthlyListeners)} monthly listeners"
                fansCount > 0L -> "${formatFansCount(fansCount)} total listeners"
                else -> ""
            }
            if (listenerText.isNotBlank()) {
                Text(
                    text = listenerText,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(MimikyuSpacing.sm))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                AsyncImage(
                    model = if (artist.avatar.contains("126.net")) "${artist.avatar}?param=100y100" else artist.avatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                )

                Spacer(Modifier.width(12.dp))

                // Follow
                Box(
                    modifier = Modifier
                        .pressable(
                            style = MimikyuPress.Action,
                            shape = RoundedCornerShape(PillRadius),
                            onClick = onFollowClick
                        )
                        .border(
                            border = if (isFollowed) BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                            else BorderStroke(0.dp, Color.Transparent),
                            shape = RoundedCornerShape(PillRadius)
                        )
                        .background(if (isFollowed) Color.Transparent else MaterialTheme.colorScheme.primary)
                        .padding(horizontal = MimikyuSpacing.md, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isFollowed) "Following" else "Follow",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(MimikyuSpacing.md))

                MimikyuIconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                }
            }
        }

        // Play
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.Bottom)
        ) {
            // Shuffle
            MimikyuIconButton(
                onClick = onPlayAll,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Shuffle, "Shuffle", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Play
            val playInteraction = remember { MutableInteractionSource() }
            FloatingActionButton(
                onClick = onPlayAll,
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                interactionSource = playInteraction,
                modifier = Modifier
                    .pressScale(MimikyuPress.Transport, playInteraction)
                    .size(56.dp)
            ) {
                Icon(Icons.Default.PlayArrow, "Play All", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
            }
        }
    }
}
