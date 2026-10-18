package com.lin0721.linmusic.feature.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.ui.components.CoverPlaceholder
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.RadiusCompact
import com.lin0721.linmusic.core.ui.theme.TextGray
import com.lin0721.linmusic.feature.music.domain.MusicStyle
import com.lin0721.linmusic.feature.music.domain.StyleHead
import com.lin0721.linmusic.feature.music.domain.StylePortrait
import com.lin0721.linmusic.feature.music.domain.StylePreference

internal val MusicEdgePadding = 20.dp

// of Genres，
private val FallbackStyleColor = Color(0xFF4A4A4A)


private val StyleHeaderHeight = 150.dp

// hex  Color，
internal fun String?.toStyleColor(): Color {
    val hex = this ?: return FallbackStyleColor
    return runCatching { Color(android.graphics.Color.parseColor("#$hex")) }.getOrDefault(FallbackStyleColor)
}

// Genres。 colorDeep， of ，
// Genres of ，
@Composable
fun MusicStyleChips(
    styles: List<MusicStyle>,
    selection: StyleSelection,
    showPreference: Boolean,
    onSelect: (StyleSelection) -> Unit
) {
    LazyRow(
        // and  of 「All /  / 」，
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        contentPadding = PaddingValues(horizontal = MusicEdgePadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showPreference) {
            item(key = "preference") {
                StyleChip(
                    text = "My Tastes",
                    color = MaterialTheme.colorScheme.primary,
                    selected = selection is StyleSelection.Preference,
                    onClick = { onSelect(StyleSelection.Preference) }
                )
            }
        }
        items(styles, key = { it.id }) { style ->
            StyleChip(
                text = style.name,
                color = style.colorHex.toStyleColor(),
                selected = (selection as? StyleSelection.Style)?.id == style.id,
                onClick = { onSelect(StyleSelection.Style(style.id)) }
            )
        }
    }
}

@Composable
private fun StyleChip(
    text: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .pressable(MimikyuPress.Pill) { onClick() }
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) color else color.copy(alpha = 0.45f))
            .then(
                if (selected) Modifier.border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(9.dp))
                else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.75f),
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

// Genres。，
@Composable
fun MusicSubStyleChips(
    children: List<MusicStyle>,
    selectedChildId: Long?,
    onSelect: (Long?) -> Unit
) {
    if (children.isEmpty()) return

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(top = 13.dp),
        contentPadding = PaddingValues(horizontal = MusicEdgePadding),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item(key = "all") {
            SubStyleChip(text = "All", selected = selectedChildId == null, onClick = { onSelect(null) })
        }
        items(children, key = { it.id }) { child ->
            SubStyleChip(
                text = child.name,
                selected = selectedChildId == child.id,
                onClick = { onSelect(child.id) }
            )
        }
    }
}

@Composable
private fun SubStyleChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .pressable(MimikyuPress.Pill) { onClick() }
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(Color.White)
                else Modifier.border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            )
            .padding(horizontal = 11.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color(0xFF121212) else Color(0xFFBDBDBD),
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// Genres。 of ，
@Composable
fun MusicPortraitCard(portrait: StylePortrait, accent: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MusicEdgePadding, vertical = 15.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.35f))))
            .padding(16.dp)
    ) {
        Text(
            text = "Your Genre Profile",
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = portrait.content,
            color = Color.White,
            fontSize = 14.5.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 9.dp)
        )
        if (portrait.dataTip.isNotBlank()) {
            Text(
                text = portrait.dataTip,
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

// Preferences
@Composable
fun MusicPreferenceBars(preferences: List<StylePreference>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = MusicEdgePadding)) {
        preferences.forEach { pref ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pref.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(62.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.09f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(pref.ratio.coerceIn(0, 100) / 100f)
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(pref.colorHex.toStyleColor())
                    )
                }
                Text(
                    text = "${pref.ratio}%",
                    color = TextGray,
                    fontSize = 11.5.sp,
                    modifier = Modifier.width(40.dp).padding(start = 10.dp)
                )
            }
        }
    }
}

// Genres： +  +  +
@Composable
fun MusicStyleHeader(head: StyleHead, onPlay: () -> Unit) {
    val accent = head.colorHex.toStyleColor()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MusicEdgePadding, vertical = 14.dp)
            .height(StyleHeaderHeight)
            .clip(RoundedCornerShape(RadiusCompact))
            .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.4f))))
    ) {
        head.coverUrl?.let {
            SubcomposeAsyncImage(
                model = it,
                contentDescription = head.name,
                modifier = Modifier.fillMaxWidth().height(StyleHeaderHeight),
                contentScale = ContentScale.Crop,
                alpha = 0.42f,
                loading = { CoverPlaceholder() },
                error = { CoverPlaceholder() }
            )
        }

        if (head.enName.isNotBlank()) {
            Text(
                text = head.enName,
                color = Color.White.copy(alpha = 0.17f),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.TopEnd).padding(14.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
                // of Play，
                .fillMaxWidth(0.82f)
        ) {
            Text(
                text = head.name,
                color = Color.White,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // ；Genres of Back 999999+ ，
            // ，
            val subtitle = head.desc.takeIf { it.isNotBlank() } ?: head.realStatsOrEmpty()
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
                .size(40.dp)
                .pressable(MimikyuPress.Icon) { onPlay() }
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Play",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// +  of （999999+ / 1000+），Genres，
private fun StyleHead.realStatsOrEmpty(): String = listOfNotNull(
    songNum.takeIf { it.isNotBlank() && !it.endsWith("+") }?.let { "$it songs" },
    artistNum.takeIf { it.isNotBlank() && !it.endsWith("+") }?.let { "$it artists" }
).joinToString(" · ")

// 「Genres」，。
// Genres：colorDeep  of ，Current
// （ 23303B、 111111 ）。
@Composable
fun MusicFavouriteSongCard(track: Track, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MusicEdgePadding)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SubcomposeAsyncImage(
            model = track.al.picUrl,
            contentDescription = track.name,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(RadiusCompact)),
            contentScale = ContentScale.Crop,
            loading = { CoverPlaceholder() },
            error = { CoverPlaceholder() }
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = "Your favorite in this genre",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = track.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = track.ar.joinToString("/") { it.name },
                color = TextGray,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Rounded.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}
