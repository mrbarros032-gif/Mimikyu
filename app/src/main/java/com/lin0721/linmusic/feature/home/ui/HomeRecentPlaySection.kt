package com.lin0721.linmusic.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.lin0721.linmusic.core.ui.components.CoverPlaceholder
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.LocalMimikyuOrientationClass
import com.lin0721.linmusic.core.ui.theme.LocalMimikyuWindowSizeClass
import com.lin0721.linmusic.core.ui.theme.MimikyuOrientationClass
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuWindowSizeClass
import com.lin0721.linmusic.core.ui.theme.RadiusCompact
import com.lin0721.linmusic.core.ui.theme.RobotoFontFamily
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.feature.recent.domain.RecentPlaylist

// ， 3 ； 2 、 3 、 4
private const val MAX_ROWS = 3
private val RowHeight = 56.dp
private val ItemGap = 9.dp

@Composable
fun RecentPlaySection(
    items: List<RecentPlaylist>,
    onClick: (RecentPlaylist) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val windowSizeClass = LocalMimikyuWindowSizeClass.current
    val orientationClass = LocalMimikyuOrientationClass.current
    val columns = when {
        windowSizeClass == MimikyuWindowSizeClass.Expanded && orientationClass == MimikyuOrientationClass.Landscape -> 4
        windowSizeClass == MimikyuWindowSizeClass.Expanded -> 3
        else -> 2
    }

    val shownItems = items.take(columns * MAX_ROWS)

    Column(modifier = modifier.fillMaxWidth().padding(top = MimikyuSpacing.sm)) {
        Text(
            text = "Recently Played",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontFamily = RobotoFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = HomeEdgePadding, end = HomeEdgePadding, bottom = 13.dp)
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HomeEdgePadding),
            horizontalArrangement = Arrangement.spacedBy(ItemGap),
            verticalArrangement = Arrangement.spacedBy(ItemGap),
            maxItemsInEachRow = columns
        ) {
            shownItems.forEach { item ->
                key(item.id) {
                    RecentPlayRow(
                        item = item,
                        modifier = Modifier
                            .weight(1f)
                            .homeReflowBounds(),
                        onClick = { onClick(item) }
                    )
                }
            }
            // ，
            repeat((columns - shownItems.size % columns) % columns) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RecentPlayRow(
    item: RecentPlaylist,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(RowHeight)
            .pressable(MimikyuPress.Card) { onClick() }
            .clip(RoundedCornerShape(RadiusCompact))
            .background(Color.White.copy(alpha = 0.08f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SubcomposeAsyncImage(
            model = item.coverUrl.withCoverParam("200y200"),
            contentDescription = null,
            modifier = Modifier.size(RowHeight),
            contentScale = ContentScale.Crop,
            loading = { CoverPlaceholder() },
            error = { CoverPlaceholder() }
        )
        Text(
            text = item.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontFamily = RobotoFontFamily,
            fontWeight = FontWeight.Medium,
            lineHeight = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 9.dp, end = 8.dp)
        )
    }
}
