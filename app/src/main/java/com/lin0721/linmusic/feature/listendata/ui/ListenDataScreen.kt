package com.lin0721.linmusic.feature.listendata.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.LocalBottomOverlayInset
import com.lin0721.linmusic.core.ui.components.EmptyState
import com.lin0721.linmusic.core.ui.components.ErrorState
import com.lin0721.linmusic.core.ui.components.FilterChipsRow
import com.lin0721.linmusic.core.ui.components.SearchResultRowSkeleton
import com.lin0721.linmusic.core.ui.components.SecondaryScreenScaffold
import com.lin0721.linmusic.core.ui.components.SongRow
import com.lin0721.linmusic.core.ui.components.SongRowData
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.DataEnterSpec
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import org.koin.androidx.compose.koinViewModel

private const val SKELETON_ROW_COUNT = 6
private const val COLLAPSED_RANK_COUNT = 5

// Tab ，
private val TabSwitchSpec = tween<Float>(220, easing = FastOutSlowInEasing)
private val TabSwitchOffsetSpec = tween<IntOffset>(220, easing = FastOutSlowInEasing)

@Composable
fun ListenDataScreen(
    viewModel: ListenDataViewModel = koinViewModel(),
    onBack: () -> Unit,
    onArtistClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTrack by viewModel.playerManager.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()

    SecondaryScreenScaffold(title = "Listening History", onBack = onBack) {
        FilterChipsRow(
            items = ListenTab.entries.map { it.label },
            selectedIndex = uiState.selectedTab.ordinal,
            onSelected = { index -> viewModel.selectTab(ListenTab.entries[index]) }
        )

        // ，，
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = uiState,
                // of ， of  of
                transitionSpec = {
                    val forward = targetState.selectedTab.ordinal >= initialState.selectedTab.ordinal
                    val offset = if (forward) 1 else -1
                    (slideInHorizontally(TabSwitchOffsetSpec) { it / 6 * offset } + fadeIn(TabSwitchSpec))
                        .togetherWith(
                            slideOutHorizontally(TabSwitchOffsetSpec) { -it / 6 * offset } +
                                fadeOut(TabSwitchSpec)
                        )
                },
                // Tab  and  of （）
                contentKey = { it.selectedTab to it.content },
                label = "listen_data_content"
            ) { state ->
                when (val content = state.content) {
                    ListenDataContent.Loading -> {
                        Column(modifier = Modifier.padding(top = MimikyuSpacing.md)) {
                            repeat(SKELETON_ROW_COUNT) { SearchResultRowSkeleton() }
                        }
                    }

                    is ListenDataContent.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            ErrorState(message = content.message, onRetry = { viewModel.retry() })
                        }
                    }

                    is ListenDataContent.Today -> TodayList(
                        content = content,
                        currentTrackId = currentTrack?.mediaId,
                        isPlaying = isPlaying,
                        onSongClick = { viewModel.playSongAt(it) }
                    )

                    is ListenDataContent.Period -> PeriodList(
                        content = content,
                        // of ， Tab  of
                        totalSeconds = state.totalSeconds,
                        periodLabel = state.selectedTab.label,
                        currentTrackId = currentTrack?.mediaId,
                        isPlaying = isPlaying,
                        onSongClick = { viewModel.playSongAt(it) },
                        onHighlightClick = { viewModel.playHighlight(it) },
                        onArtistClick = onArtistClick
                    )

                    is ListenDataContent.Year -> YearList(content)
                }
            }
        }
    }
}

// ：， LazyColumn  item
@Composable
private fun rememberDataEnterProgress(key: Any?): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(key) {
        progress.snapTo(0f)
        progress.animateTo(1f, DataEnterSpec)
    }
    return progress.value
}

// =======================  =======================

@Composable
private fun TodayList(
    content: ListenDataContent.Today,
    currentTrackId: String?,
    isPlaying: Boolean,
    onSongClick: (Int) -> Unit
) {
    if (content.songs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Rounded.Insights,
                title = "No listening history today",
                subtitle = "Listen to some songs and come back later"
            )
        }
        return
    }

    LazyColumn(contentPadding = bottomInsetPadding()) {
        paddedItem("today_header") {
            Column(modifier = Modifier.fillMaxWidth().padding(top = MimikyuSpacing.sm)) {
                Text(
                    text = "Played Today",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    text = "${content.songs.size} songs",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 34.sp
                )
            }
        }
        itemsIndexed(content.songs.size) { index ->
            val song = content.songs[index]
            SongRow(
                data = SongRowData(
                    id = song.id,
                    title = song.name,
                    artist = song.artistName,
                    coverUrl = song.coverUrl
                ),
                isActive = currentTrackId == song.id.toString(),
                isPlaying = isPlaying,
                index = index + 1,
                onClick = { onSongClick(index) }
            )
        }
    }
}

// =======================  /  =======================

@Composable
private fun PeriodList(
    content: ListenDataContent.Period,
    totalSeconds: Long,
    periodLabel: String,
    currentTrackId: String?,
    isPlaying: Boolean,
    onSongClick: (Int) -> Unit,
    onHighlightClick: (Long) -> Unit,
    onArtistClick: (Long) -> Unit
) {
    val report = content.report
    val rank = content.rank
    var expanded by rememberSaveable(periodLabel) { mutableStateOf(false) }
    val rankSongs = rank?.songs.orEmpty()
    val visibleSongs = if (expanded) rankSongs else rankSongs.take(COLLAPSED_RANK_COUNT)
    val enterProgress = rememberDataEnterProgress(content)
    // ， ViewModel； Tab  periodLabel Reset
    var selectedDay by rememberSaveable(periodLabel) { mutableStateOf<Int?>(null) }
    var selectedPeriod by rememberSaveable(periodLabel) { mutableStateOf<Int?>(null) }

    if ((report == null || (report.playMinutes == 0 && report.songCount == 0)) && rankSongs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Rounded.Insights,
                title = "No listening history for this $periodLabel",
                subtitle = "Listen to some songs and come back later"
            )
        }
        return
    }

    LazyColumn(contentPadding = bottomInsetPadding()) {
        if (report != null) {
            paddedItem("hero") {
                HeroSection(
                    playMinutes = report.playMinutes,
                    beyondPercentText = report.beyondPercentText,
                    achievementTitle = report.achievementTitle,
                    achievementSubtitle = report.achievementSubtitle,
                    listenDays = report.listenDays,
                    songCount = report.songCount,
                    wallpaperUrls = report.wallpaperUrls,
                    totalSeconds = totalSeconds,
                    periodLabel = periodLabel,
                    progress = enterProgress
                )
            }
            paddedItem("highlight") {
                HighlightSection(report.highlights, periodLabel, onHighlightClick)
            }
            paddedItem("daily") {
                DailyChartSection(
                    days = report.dailyDurations,
                    progress = enterProgress,
                    selectedIndex = selectedDay,
                    onSelect = { selectedDay = it }
                )
            }
            paddedItem("period") {
                TimePeriodSection(
                    periods = report.timePeriods,
                    progress = enterProgress,
                    selectedIndex = selectedPeriod,
                    onSelect = { selectedPeriod = it }
                )
            }
        }

        if (rankSongs.isNotEmpty()) {
            paddedItem("rank_title") {
                SectionTitle("Most Played", "${rank?.totalCount ?: rankSongs.size} songs total")
            }
            itemsIndexed(visibleSongs.size) { index ->
                val song = visibleSongs[index]
                SongRow(
                    data = SongRowData(
                        id = song.id,
                        title = song.name,
                        artist = song.artistName,
                        coverUrl = song.coverUrl,
                        durationText = "${song.playCount} plays"
                    ),
                    isActive = currentTrackId == song.id.toString(),
                    isPlaying = isPlaying,
                    index = index + 1,
                    onClick = { onSongClick(index) }
                )
            }
            if (rankSongs.size > COLLAPSED_RANK_COUNT) {
                item(key = "rank_expand") {
                    Text(
                        text = if (expanded) "Show less" else "Show all ${rankSongs.size} songs",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressable(MimikyuPress.Row) { expanded = !expanded }
                            .padding(vertical = MimikyuSpacing.sm + MimikyuSpacing.xs)
                    )
                }
            }
        }

        if (report != null) {
            paddedItem("artists") { TopArtistsSection(report.topArtists, onArtistClick) }
            paddedItem("preference") {
                PreferenceSection(report.genreName, report.ageLabel, report.languageName)
            }
            paddedItem("podcast") {
                PodcastSection(
                    podcastMinutes = report.podcastMinutes,
                    podcastEpisodes = report.podcastEpisodes,
                    progress = enterProgress
                )
            }

        }
    }
}

// =======================  =======================

@Composable
private fun YearList(content: ListenDataContent.Year) {
    if (content.stats.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(icon = Icons.Rounded.Insights, title = "No yearly listening history yet")
        }
        return
    }

    val enterProgress = rememberDataEnterProgress(content)
    LazyColumn(contentPadding = bottomInsetPadding()) {
        paddedItem("years") { YearStatsSection(content.stats, enterProgress) }
    }
}

// =======================  =======================

// ，SongRow  padding，
@Composable
private fun bottomInsetPadding(): PaddingValues = PaddingValues(
    top = MimikyuSpacing.sm,
    bottom = LocalBottomOverlayInset.current + 16.dp
)

private fun LazyListScope.paddedItem(key: String, content: @Composable () -> Unit) {
    item(key = key) {
        Column(modifier = Modifier.padding(horizontal = MimikyuSpacing.md)) { content() }
    }
}

private fun LazyListScope.itemsIndexed(count: Int, itemContent: @Composable (Int) -> Unit) {
    items(count = count, key = { "row_$it" }) { index -> itemContent(index) }
}
