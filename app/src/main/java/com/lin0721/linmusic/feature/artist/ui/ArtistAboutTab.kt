package com.lin0721.linmusic.feature.artist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.model.ArtistDetailInfo
import com.lin0721.linmusic.core.model.ArtistInfo
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

// About Tab:  + Recommended
fun LazyListScope.artistAboutTab(
    artist: ArtistDetailInfo,
    fansCount: Long = 0L,
    playCount: Long = 0L,
    mimikyuMonthlyListeners: Long = 0L,
    similarArtists: List<ArtistInfo>,
    onShowBioDialog: () -> Unit,
    onArtistClick: (Long) -> Unit
) {
    if (fansCount > 0L || playCount > 0L || mimikyuMonthlyListeners > 0L) {
        item(key = "artist_listeners_stats") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = MimikyuSpacing.md, vertical = 12.dp)
            ) {
                Text(
                    text = "Listeners & Activity",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = formatExactCount(fansCount),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Total Listeners",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (mimikyuMonthlyListeners > 0L) "Includes Mimikyu monthly listeners" else "Unique Last.fm scrobblers",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (mimikyuMonthlyListeners > 0L) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = formatExactCount(mimikyuMonthlyListeners),
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Mimikyu Monthly",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Monthly active listeners",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else if (playCount > 0L) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = formatExactCount(playCount),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Total Scrobbles",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Aggregated plays count",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                if (mimikyuMonthlyListeners > 0L && playCount > 0L) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = formatExactCount(playCount),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Total Scrobbles",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Aggregated plays count from Last.fm",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (!artist.identifyTag.isNullOrEmpty()) {
        item(key = "artist_genres") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = MimikyuSpacing.md, vertical = 8.dp)
            ) {
                Text(
                    text = "Genres & Styles",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    artist.identifyTag.forEach { genre ->
                        androidx.compose.material3.Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    item(key = "bio_content") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = MimikyuSpacing.md, vertical = 20.dp)
        ) {
            Text(
                text = "Artist Bio",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onShowBioDialog() }
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = artist.briefDesc.ifBlank { "No artist biography available" }.trim(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (artist.briefDesc.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tap to read full bio",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
    if (similarArtists.isNotEmpty()) {
        item(key = "similar_artists_section") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = MimikyuSpacing.md)
            ) {
                Text(
                    text = "Similar Artists",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = MimikyuSpacing.md, end = MimikyuSpacing.md, bottom = 12.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = MimikyuSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(MimikyuSpacing.md)
                ) {
                    items(similarArtists, key = { it.id }) { artistInfo ->
                        SimilarArtistCard(artist = artistInfo, onClick = { onArtistClick(artistInfo.id) })
                    }
                }
            }
        }
    }
}
