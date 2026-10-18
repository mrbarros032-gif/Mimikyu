package com.lin0721.linmusic.feature.newworks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.lin0721.linmusic.LocalBottomOverlayInset
import com.lin0721.linmusic.core.ui.components.CoverPlaceholder
import com.lin0721.linmusic.core.ui.components.EmptyState
import com.lin0721.linmusic.core.ui.components.ErrorState
import com.lin0721.linmusic.core.ui.components.shimmerBackground
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.NeteaseRed
import com.lin0721.linmusic.core.ui.theme.RadiusCompact
import com.lin0721.linmusic.core.ui.theme.rememberMimikyuGridColumns
import com.lin0721.linmusic.feature.newworks.domain.NewWorksMv
import com.lin0721.linmusic.feature.newworks.domain.NewWorksRelease
import java.util.Locale

// ，（ 157 ）
private const val BULKY_RELEASE_TRACK_THRESHOLD = 50
private val HeroHeight = 168.dp
private val RailCardWidth = 104.dp
private val RailCardHeight = 58.dp

// tab「」 of ： MV  +  MV  + New Releases。
// of /Back—— HomeSharedHeader ，。
@Composable
fun NewWorksFeedContent(
    uiState: NewWorksUiState,
    onMvClick: (Long, String) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onSongPlay: (NewWorksRelease) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit
) {
    when (uiState) {
        NewWorksUiState.Loading -> NewWorksFeedSkeleton()

        is NewWorksUiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ErrorState(message = uiState.message, onRetry = onRetry)
        }

        is NewWorksUiState.Success -> {
            if (uiState.mvs.isEmpty() && uiState.releases.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Rounded.NewReleases,
                        title = "No new releases from followed artists yet",
                        subtitle = "Follow artists to see their new tracks and MVs here"
                    )
                }
                return
            }
            NewWorksFeedGrid(
                state = uiState,
                onMvClick = onMvClick,
                onAlbumClick = onAlbumClick,
                onSongPlay = onSongPlay,
                onLoadMore = onLoadMore
            )
        }
    }
}

@Composable
private fun NewWorksFeedGrid(
    state: NewWorksUiState.Success,
    onMvClick: (Long, String) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onSongPlay: (NewWorksRelease) -> Unit,
    onLoadMore: () -> Unit
) {
    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember(state.releases.size, state.hasMore, state.isLoadingMore) {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            // 0  MV ，release  of  grid index  +1
            lastVisible >= state.releases.size - 2 && state.hasMore && !state.isLoadingMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    val columns = rememberMimikyuGridColumns(compact = 2, expandedPortrait = 4, expandedLandscape = 6)
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(MimikyuSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MimikyuSpacing.md),
        contentPadding = PaddingValues(
            start = MimikyuSpacing.md,
            end = MimikyuSpacing.md,
            top = MimikyuSpacing.sm,
            bottom = LocalBottomOverlayInset.current + 16.dp
        ),
        modifier = Modifier.fillMaxSize()
    ) {

        if (state.releases.isNotEmpty()) {
            item(key = "release_header", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "New Releases",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = MimikyuSpacing.xs)
                )
            }

            items(state.releases, key = { "${it.isAlbum}_${it.id}" }) { release ->
                ReleaseGridCard(
                    release = release,
                    onClick = {
                        if (release.isAlbum) onAlbumClick(release.id) else onSongPlay(release)
                    }
                )
            }
        }

        if (state.isLoadingMore) {
            item(key = "loading_more", span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = MimikyuSpacing.md), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

// MV  + ：，
@Composable
private fun MvSection(mvs: List<NewWorksMv>, onMvClick: (Long, String) -> Unit) {
    val hero = mvs.first()
    val rest = mvs.drop(1)

    Column(modifier = Modifier.padding(bottom = MimikyuSpacing.md)) {
        HeroMvCard(mv = hero, onClick = { onMvClick(hero.id, hero.name) })

        if (rest.isNotEmpty()) {
            Spacer(Modifier.height(MimikyuSpacing.md))
            Text(
                text = "More New MVs",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = MimikyuSpacing.xs)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(MimikyuSpacing.sm)) {
                lazyRowItems(rest, key = { it.id }) { mv ->
                    RailMvCard(mv = mv, onClick = { onMvClick(mv.id, mv.name) })
                }
            }
        }
    }
}

@Composable
private fun HeroMvCard(mv: NewWorksMv, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeroHeight)
            .pressable(MimikyuPress.Card, onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
    ) {
        SubcomposeAsyncImage(
            model = "${mv.coverUrl}?param=600y340",
            contentDescription = mv.name,
            contentScale = ContentScale.Crop,
            loading = { CoverPlaceholder() },
            error = { CoverPlaceholder() },
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
        )
        Text(
            text = "Latest MVs",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.TopStart).padding(MimikyuSpacing.sm)
        )
        if (mv.durationMs > 0) {
            Text(
                text = formatMvDuration(mv.durationMs),
                color = Color.White,
                fontSize = 9.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(MimikyuSpacing.sm)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = MimikyuSpacing.sm)
                .padding(bottom = MimikyuSpacing.sm)
        ) {
            Text(
                text = mv.name,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = mv.artistName,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RailMvCard(mv: NewWorksMv, onClick: () -> Unit) {
    Column(modifier = Modifier.width(RailCardWidth).pressable(MimikyuPress.Card, onClick = onClick)) {
        Box(
            modifier = Modifier
                .width(RailCardWidth)
                .height(RailCardHeight)
                .clip(RoundedCornerShape(RadiusCompact))
        ) {
            SubcomposeAsyncImage(
                model = "${mv.coverUrl}?param=300y170",
                contentDescription = mv.name,
                contentScale = ContentScale.Crop,
                loading = { CoverPlaceholder() },
                error = { CoverPlaceholder() },
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(26.dp)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            if (mv.durationMs > 0) {
                Text(
                    text = formatMvDuration(mv.durationMs),
                    color = Color.White,
                    fontSize = 8.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = mv.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = mv.artistName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReleaseGridCard(release: NewWorksRelease, onClick: () -> Unit) {
    val isBulky = release.trackCount > BULKY_RELEASE_TRACK_THRESHOLD
    Column(modifier = Modifier.pressable(MimikyuPress.Card, onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
        ) {
            SubcomposeAsyncImage(
                model = "${release.coverUrl}?param=300y300",
                contentDescription = release.title,
                contentScale = ContentScale.Crop,
                loading = { CoverPlaceholder() },
                error = { CoverPlaceholder() },
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = release.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = release.artistName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (release.isAlbum) "${release.trackCount} tracks total" else "Tracks",
            color = if (isBulky) NeteaseRed.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 8.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .then(
                    if (isBulky) {
                        Modifier.background(NeteaseRed.copy(alpha = 0.12f))
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun NewWorksFeedSkeleton() {
    Column(modifier = Modifier.fillMaxSize().padding(MimikyuSpacing.md)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HeroHeight)
                .shimmerBackground(RoundedCornerShape(14.dp))
        )
        Spacer(Modifier.height(MimikyuSpacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(MimikyuSpacing.sm)) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .width(RailCardWidth)
                        .height(RailCardHeight)
                        .shimmerBackground(RoundedCornerShape(RadiusCompact))
                )
            }
        }
        Spacer(Modifier.height(MimikyuSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MimikyuSpacing.sm)
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .shimmerBackground(RoundedCornerShape(10.dp))
                )
            }
        }
    }
}

// mm:ss 。ArtistScreenComponents  of  private ，
// （PlaylistCategoryScreen  of  formatPlayCount ），
private fun formatMvDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
