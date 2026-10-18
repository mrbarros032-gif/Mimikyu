package com.lin0721.linmusic.feature.listendata.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.lin0721.linmusic.core.ui.components.CoverPlaceholder
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.BackgroundDark
import com.lin0721.linmusic.core.ui.theme.DataEnterStaggerFraction
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.RadiusCompact
import com.lin0721.linmusic.feature.listendata.domain.DayDuration
import com.lin0721.linmusic.feature.listendata.domain.FriendListening
import com.lin0721.linmusic.feature.listendata.domain.Highlight
import com.lin0721.linmusic.feature.listendata.domain.TimePeriod
import com.lin0721.linmusic.feature.listendata.domain.TopArtist
import com.lin0721.linmusic.feature.listendata.domain.YearStat
import com.lin0721.linmusic.feature.listendata.domain.toHours
import com.lin0721.linmusic.feature.listendata.domain.toHoursText

private val CardShape = RoundedCornerShape(10.dp)
private val CardColor = Color.White.copy(alpha = 0.06f)
private val TrackColor = Color.White.copy(alpha = 0.06f)
private val BarMaxHeight = 72.dp
private val BarMinHeight = 3.dp

private const val CoverWallCount = 6
private val BarValueGap = 2.dp

internal fun itemProgress(total: Float, index: Int, count: Int): Float {
    if (count <= 1) return total
    val start = DataEnterStaggerFraction * index / (count - 1)
    val span = 1f - DataEnterStaggerFraction
    return ((total - start) / span).coerceIn(0f, 1f)
}

internal fun Int.byProgress(progress: Float): Int = (this * progress).toInt()

// =======================  =======================

@Composable
fun SectionTitle(text: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MimikyuSpacing.lg, bottom = MimikyuSpacing.sm),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        if (!trailing.isNullOrBlank()) {
            Text(text = trailing, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RowScope.StatCard(
    label: String,
    value: String,
    unit: String,
    caption: String? = null,
    icon: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(CardShape)
            .background(CardColor)
            .padding(MimikyuSpacing.sm + MimikyuSpacing.xs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                icon()
                Spacer(Modifier.width(MimikyuSpacing.xs))
            }
            Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Spacer(Modifier.height(MimikyuSpacing.xxs))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(MimikyuSpacing.xs))
            Text(
                text = unit,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        if (!caption.isNullOrBlank()) {
            Text(text = caption, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

// =======================  =======================

@Composable
private fun BoxScope.CoverWall(urls: List<String>) {
    if (urls.isEmpty()) return

    Row(modifier = Modifier.matchParentSize()) {
        urls.take(CoverWallCount).forEach { url ->
            SubcomposeAsyncImage(
                model = "$url?param=200y200",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = {},
                error = {},
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(
                Brush.verticalGradient(
                    0f to BackgroundDark.copy(alpha = 0.95f),
                    0.35f to BackgroundDark.copy(alpha = 0.86f),
                    0.8f to BackgroundDark.copy(alpha = 0.93f),
                    1f to BackgroundDark
                )
            )
    )
}

@Composable
fun HeroSection(
    playMinutes: Int,
    beyondPercentText: String,
    achievementTitle: String,
    achievementSubtitle: String,
    listenDays: Int,
    songCount: Int,
    wallpaperUrls: List<String>,
    totalSeconds: Long,
    periodLabel: String,
    progress: Float
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            CoverWall(wallpaperUrls)

            Column(modifier = Modifier.padding(top = MimikyuSpacing.lg, bottom = MimikyuSpacing.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$periodLabel Listening",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    if (songCount > 0) {
                        Text(
                            text = " · $songCount songs listened",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(top = MimikyuSpacing.xxs)
                ) {
                    Text(
                        text = playMinutes.byProgress(progress).toString(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "mins",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = MimikyuSpacing.xs, bottom = 7.dp)
                    )
                }

                if (beyondPercentText.isNotBlank()) {
                    val percent = beyondPercentText.filter { it.isDigit() }.toIntOrNull() ?: 0
                    Spacer(Modifier.height(MimikyuSpacing.sm))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(TrackColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(percent.coerceIn(0, 100) / 100f * progress)
                                .height(4.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Text(
                        text = beyondPercentText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = MimikyuSpacing.xs)
                    )
                }
            }
        }

        if (achievementTitle.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MimikyuSpacing.md)
                .clip(CardShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .padding(MimikyuSpacing.sm + MimikyuSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(MimikyuSpacing.sm))
                Column {
                    Text(
                        text = achievementTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (achievementSubtitle.isNotBlank()) {
                        Text(
                            text = achievementSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = MimikyuSpacing.sm + MimikyuSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(MimikyuSpacing.sm)
        ) {
            StatCard(
                label = "Days Listened",
                value = listenDays.byProgress(progress).toString(),
                unit = "days"
            )
            StatCard(
                label = "Total Listening",
                value = toHours(totalSeconds).toInt().byProgress(progress).toString(),
                unit = "hrs"
            )
        }
    }
}

// =======================  =======================

@Composable
fun HighlightSection(highlights: List<Highlight>, periodLabel: String, onSongClick: (Long) -> Unit) {
    if (highlights.isEmpty()) return

    SectionTitle("$periodLabel Highlights")
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(MimikyuSpacing.sm),
        contentPadding = PaddingValues(vertical = MimikyuSpacing.xxs)
    ) {
        items(highlights.size) { index ->
            val highlight = highlights[index]
            Column(
                modifier = Modifier
                    .width(132.dp)
                    .clip(CardShape)
                    .background(CardColor)
                    .pressable(MimikyuPress.Card) { onSongClick(highlight.songId) }
                    .padding(MimikyuSpacing.sm + MimikyuSpacing.xs)
            ) {
                SubcomposeAsyncImage(
                    model = "${highlight.coverUrl}?param=300y300",
                    contentDescription = highlight.songName,
                    contentScale = ContentScale.Crop,
                    loading = { CoverPlaceholder() },
                    error = { CoverPlaceholder() },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(RadiusCompact))
                )
                Spacer(Modifier.height(MimikyuSpacing.sm))
                Text(
                    text = highlight.label,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = highlight.songName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = MimikyuSpacing.xxs)
                )
                if (highlight.valueText.isNotBlank()) {
                    Text(
                        text = highlight.valueText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(top = MimikyuSpacing.xxs)
                    )
                }
            }
        }
    }
}

// =======================  =======================

@Composable
fun DailyChartSection(
    days: List<DayDuration>,
    progress: Float,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit
) {
    if (days.isEmpty()) return
    val maxMinutes = days.maxOf { it.minutes }.coerceAtLeast(1)
    val selected = selectedIndex?.let { days.getOrNull(it) }

    SectionTitle(
        text = "Daily Trend",
        trailing = selected?.let { "${it.fullLabel} · ${it.minutes} mins" }
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(if (days.size > 10) 2.dp else MimikyuSpacing.xs)
    ) {
        days.forEachIndexed { index, day ->
            val grow = itemProgress(progress, index, days.size)
            val ratio = day.minutes.toFloat() / maxMinutes * grow
            val isPeak = day.minutes == maxMinutes
            val isSelected = selectedIndex == index
            val barColor = when {
                isSelected -> MaterialTheme.colorScheme.primary
                selectedIndex != null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                isPeak -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(if (isSelected) null else index) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val showValue = if (selectedIndex == null) isPeak else isSelected
                Text(
                    text = "${day.minutes.byProgress(grow)}m",
                    color = if (showValue) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    fontSize = 10.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .wrapContentWidth(unbounded = true)
                        .padding(bottom = BarValueGap)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BarMaxHeight * ratio + BarMinHeight)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(barColor)
                )
                Text(
                    text = day.label,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = MimikyuSpacing.xs)
                        .wrapContentWidth(unbounded = true)
                )
            }
        }
    }
}

// =======================  =======================

private fun periodIcon(key: String): ImageVector = when (key) {
    "early_morning" -> Icons.Rounded.Bedtime
    "morning" -> Icons.Rounded.WbSunny
    "noon" -> Icons.Rounded.LightMode
    "afternoon" -> Icons.Rounded.WbTwilight
    "night" -> Icons.Rounded.NightsStay
    "deep_night" -> Icons.Rounded.DarkMode
    else -> Icons.Rounded.Schedule
}

@Composable
fun TimePeriodSection(
    periods: List<TimePeriod>,
    progress: Float,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit
) {
    if (periods.isEmpty()) return
    val sorted = periods.sortedByDescending { it.minutes }
    val maxMinutes = sorted.first().minutes.coerceAtLeast(1)
    val totalMinutes = sorted.sumOf { it.minutes }
    val selected = selectedIndex?.let { sorted.getOrNull(it) }

    SectionTitle(
        text = "Time Distribution",
        trailing = selected?.let {
            if (totalMinutes > 0) "${it.label} ${it.minutes * 100 / totalMinutes}%" else it.label
        }
    )
    sorted.forEachIndexed { index, period ->
        val grow = itemProgress(progress, index, sorted.size)
        val isSelected = selectedIndex == index
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onSelect(if (isSelected) null else index) }
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = periodIcon(period.key),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(MimikyuSpacing.xs + 2.dp))
            Text(
                text = period.label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.width(48.dp)
            )
            Spacer(Modifier.width(MimikyuSpacing.sm))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(TrackColor)
            ) {
                if (period.minutes > 0) {
                    val barColor = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        selectedIndex != null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        period.minutes == maxMinutes -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(period.minutes.toFloat() / maxMinutes * grow)
                            .height(14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(barColor)
                    )
                }
            }
            Spacer(Modifier.width(MimikyuSpacing.sm))
            Text(
                text = "${period.minutes.byProgress(grow)}m",
                color = if (isSelected || (selectedIndex == null && period.minutes == maxMinutes)) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 11.sp,
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

// =======================  of Artists =======================

@Composable
fun TopArtistsSection(artists: List<TopArtist>, onArtistClick: (Long) -> Unit) {
    if (artists.isEmpty()) return

    SectionTitle("Top Artists")
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(MimikyuSpacing.md),
        contentPadding = PaddingValues(vertical = MimikyuSpacing.xxs)
    ) {
        items(artists.size) { index ->
            val artist = artists[index]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(64.dp)
                    .pressable(MimikyuPress.Card) { onArtistClick(artist.id) }
            ) {
                SubcomposeAsyncImage(
                    model = "${artist.coverUrl}?param=200y200",
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    loading = { CoverPlaceholder() },
                    error = { CoverPlaceholder() },
                    modifier = Modifier.size(56.dp).clip(CircleShape)
                )
                Text(
                    text = artist.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = MimikyuSpacing.xs)
                )
                Text(
                    text = artist.countText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

// =======================  =======================

@Composable
fun PreferenceSection(genreName: String, ageLabel: String, languageName: String) {
    val items = listOf("Genre" to genreName, "Era" to ageLabel, "Language" to languageName)
        .filter { it.second.isNotBlank() }
    if (items.isEmpty()) return

    SectionTitle("Listening Preferences")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MimikyuSpacing.sm)
    ) {
        items.forEach { (label, value) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(CardShape)
                    .background(CardColor)
                    .padding(vertical = MimikyuSpacing.sm + MimikyuSpacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                Text(
                    text = value,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = MimikyuSpacing.xxs)
                )
            }
        }
    }
}

// =======================  =======================

@Composable
fun PodcastSection(
    podcastMinutes: Int,
    podcastEpisodes: Int,
    progress: Float
) {
    val hasPodcast = podcastMinutes > 0 || podcastEpisodes > 0
    if (!hasPodcast) return

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = MimikyuSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(MimikyuSpacing.sm)
    ) {
        StatCard(
            label = "Podcasts",
            value = podcastMinutes.byProgress(progress).toString(),
            unit = "mins",
            caption = "$podcastEpisodes episodes",
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
            }
        )
    }
}

// =======================  =======================

@Composable
fun FriendsSection(friends: List<FriendListening>, keywords: List<String>) {
    if (friends.isEmpty() && keywords.isEmpty()) return

    SectionTitle("Friends Listening")
    friends.forEach { friend ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = MimikyuSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubcomposeAsyncImage(
                model = "${friend.avatarUrl}?param=100y100",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = { CoverPlaceholder() },
                error = { CoverPlaceholder() },
                modifier = Modifier.size(36.dp).clip(CircleShape)
            )
            Spacer(Modifier.width(MimikyuSpacing.sm + MimikyuSpacing.xs))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.username,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (friend.playCount > 0) "${friend.songName} · ${friend.playCount} plays"
                    else friend.songName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (keywords.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = MimikyuSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(MimikyuSpacing.sm)
        ) {
            keywords.take(3).forEach { keyword ->
                Text(
                    text = keyword,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = MimikyuSpacing.sm + MimikyuSpacing.xs, vertical = 5.dp)
                )
            }
        }
    }
}

// ======================= Yearly History =======================

@Composable
fun YearStatsSection(stats: List<YearStat>, progress: Float) {
    if (stats.isEmpty()) return
    val maxDuration = stats.maxOf { it.durationSeconds }.coerceAtLeast(1)

    SectionTitle("Yearly History")
    stats.forEachIndexed { index, stat ->
        val grow = itemProgress(progress, index, stats.size)
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = MimikyuSpacing.xs + 2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stat.label.ifBlank { stat.year.toString() },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${stat.playCount} songs · ${toHoursText(stat.durationSeconds)} hrs",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(MimikyuSpacing.xs))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(RadiusCompact / 2))
                    .background(TrackColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(stat.durationSeconds.toFloat() / maxDuration * grow)
                        .height(7.dp)
                        .clip(RoundedCornerShape(RadiusCompact / 2))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
