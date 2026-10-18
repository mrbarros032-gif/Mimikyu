package com.lin0721.linmusic.feature.artist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

// Tab ， and  ArtistContent  of
@Composable
fun ArtistTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Music", "Albums", "About")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = MimikyuSpacing.md, vertical = 12.dp),
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
                // ：，
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
