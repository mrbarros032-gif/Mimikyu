package com.lin0721.linmusic.feature.profile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.BackgroundDark
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.PillRadius
import com.lin0721.linmusic.core.ui.theme.TextGray
import com.lin0721.linmusic.feature.profile.domain.ProfileUserInfo

// Composable for follow counts preserved for upcoming follow feature
@Composable
fun FollowCountItem(
    count: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "$count",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = TextGray,
            fontSize = 12.sp
        )
    }
}

// （////Follow）
@Composable
fun ProfileHeaderInfo(
    userInfo: ProfileUserInfo,
    isSelf: Boolean,
    onFollowClick: () -> Unit,
    onFollowsClick: () -> Unit,
    onFollowedsClick: () -> Unit,
    showFollowCounts: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 20.dp, vertical = MimikyuSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = if (userInfo.avatarUrl.contains("googleusercontent.com") || !userInfo.avatarUrl.contains("163.com")) userInfo.avatarUrl else "${userInfo.avatarUrl}?param=200y200",
            contentDescription = userInfo.nickname,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = userInfo.nickname,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (userInfo.signature.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = userInfo.signature,
                color = TextGray,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showFollowCounts) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FollowCountItem(count = userInfo.followedsCount, label = "Followers", onClick = onFollowedsClick)
                FollowCountItem(count = userInfo.followsCount, label = "Following", onClick = onFollowsClick)
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "${userInfo.playlistCount}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Playlists",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (!isSelf) {
            Spacer(modifier = Modifier.height(MimikyuSpacing.md))
            Box(
                modifier = Modifier
                    .pressable(
                        style = MimikyuPress.Action,
                        shape = RoundedCornerShape(PillRadius),
                        onClick = onFollowClick
                    )
                    .border(
                        border = if (userInfo.isFollowedByMe) BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                        else BorderStroke(0.dp, Color.Transparent),
                        shape = RoundedCornerShape(PillRadius)
                    )
                    .background(if (userInfo.isFollowedByMe) Color.Transparent else MaterialTheme.colorScheme.primary)
                    .padding(horizontal = MimikyuSpacing.lg, vertical = 8.dp)
            ) {
                Text(
                    text = if (userInfo.isFollowedByMe) "Following" else "Follow",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(MimikyuSpacing.md))

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.08f),
            thickness = 1.dp
        )
    }
}

// Tab  (Playlists and Listening Rank; Moments removed)
@Composable
fun ProfileTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Playlists", "Top Songs")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .pressable(MimikyuPress.Tab) { onTabSelected(index) }
                    .padding(vertical = MimikyuSpacing.xs)
            ) {
                Text(
                    text = title,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(28.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}
