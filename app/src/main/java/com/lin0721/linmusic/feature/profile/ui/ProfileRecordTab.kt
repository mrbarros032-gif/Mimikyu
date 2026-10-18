package com.lin0721.linmusic.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.components.EmptyState
import com.lin0721.linmusic.core.ui.components.SongRow
import com.lin0721.linmusic.core.ui.components.SongRowData
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.PillRadius
import com.lin0721.linmusic.core.ui.theme.TextGray
import com.lin0721.linmusic.feature.profile.domain.ProfileListenRankItem

private val RANK_SUB_TABS = listOf("All Time", "Past Week")

// 「Listening Rank」Tab  of ， LazyColumn  of  item ， profilePlaylistItems
fun LazyListScope.profileRecordItems(
    items: List<ProfileListenRankItem>,
    isLoading: Boolean,
    subTab: Int,
    onSubTabSelected: (Int) -> Unit,
    // of Play，
    showPlayCount: Boolean
) {
    item(key = "record_subtabs") {
        ProfileRankSubTabs(subTab = subTab, onSubTabSelected = onSubTabSelected)
    }

    if (isLoading) {
        item(key = "record_loading") {
            Box(Modifier.fillMaxSize().padding(vertical = 80.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
        }
        return
    }
    if (items.isEmpty()) {
        item(key = "record_empty") {
            EmptyState(icon = Icons.Rounded.Leaderboard, title = "No listening rank data")
        }
        return
    }

    itemsIndexed(items, key = { index, item -> "${subTab}_${item.songId}_$index" }) { index, item ->
        Box(modifier = Modifier.padding(horizontal = 4.dp)) {
            SongRow(
                data = SongRowData(
                    id = item.songId,
                    title = item.songName,
                    artist = item.artistName,
                    coverUrl = item.albumCoverUrl
                ),
                index = index + 1,
                onClick = {},
                trailingSlot = {
                    if (showPlayCount) {
                        Text(
                            text = "${item.playCount} plays",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                }
            )
        }
    }
}

// 「All Time/Past Week」 Tab： FilterChipsRow， of  20dp
// 20dp 「Playlists/Moments/Listening Rank」，；
// 20dp，
@Composable
private fun ProfileRankSubTabs(
    subTab: Int,
    onSubTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = MimikyuSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(MimikyuSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RANK_SUB_TABS.forEachIndexed { index, title ->
            val isSelected = index == subTab
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .pressable(MimikyuPress.Pill) { onSubTabSelected(index) }
                    .clip(RoundedCornerShape(PillRadius))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
