package com.lin0721.linmusic.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lin0721.linmusic.LocalBottomOverlayInset
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.ui.components.AdaptiveContentWidth
import com.lin0721.linmusic.core.ui.components.DiscoverySectionSkeleton
import com.lin0721.linmusic.core.ui.components.ErrorState
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.feature.search.domain.PlaylistTag

internal data class MimikyuBrowseItem(
    val title: String,
    val backgroundColor: Color,
    val artworkUrl: String,
    val tag: String
)

internal val discoverItems = listOf(
    MimikyuBrowseItem(
        title = "Made For\nYou",
        backgroundColor = Color(0xFF274288),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/94/d4/24/94d42473-eb0e-229b-dd84-900a1874a91d/093624931980.jpg/300x300bb.jpg",
        tag = "Made For You"
    ),
    MimikyuBrowseItem(
        title = "New\nReleases",
        backgroundColor = Color(0xFFBA5D07),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/18/75/d5/1875d587-3892-c732-8edb-e864c5a53b5b/21UMGIM11942.rgb.jpg/300x300bb.jpg",
        tag = "New Releases"
    ),
    MimikyuBrowseItem(
        title = "Mimikyu\nClassics",
        backgroundColor = Color(0xFF1E3264),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/62/93/13/6293132e-20ff-67ab-3d1f-96bb6797a6ba/196589564955.jpg/300x300bb.jpg",
        tag = "Classics"
    ),
    MimikyuBrowseItem(
        title = "Charts",
        backgroundColor = Color(0xFF8D67AB),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/22/32/f5/2232f592-b812-ee38-2ceb-3c23e43c7ddd/663918276136.jpg/300x300bb.jpg",
        tag = "Charts"
    ),
    MimikyuBrowseItem(
        title = "Trending",
        backgroundColor = Color(0xFFBA2B77),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music113/v4/4f/84/1b/4f841b0a-ac93-1275-f6b1-659fd4c8a202/13ABKIM00042.rgb.jpg/300x300bb.jpg",
        tag = "Trending"
    ),
    MimikyuBrowseItem(
        title = "Discover",
        backgroundColor = Color(0xFF7B629B),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/18/c3/ea/18c3eaae-229c-c529-634c-d6219e161ccb/5059805957458_cover.jpg/300x300bb.jpg",
        tag = "Discover"
    ),
    MimikyuBrowseItem(
        title = "Mimikyu\nSingles",
        backgroundColor = Color(0xFF484D58),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/d2/01/73/d20173bb-3589-407f-2d57-0ed80ec9c380/7954.jpg/300x300bb.jpg",
        tag = "Singles"
    ),
    MimikyuBrowseItem(
        title = "Decades",
        backgroundColor = Color(0xFFD86C00),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/e9/71/2b/e9712b92-56c5-e767-cd99-f4e9dff4dd3d/5063113338182_cover.jpg/300x300bb.jpg",
        tag = "Decades"
    )
)

internal val genreItems = listOf(
    MimikyuBrowseItem(
        title = "Pop",
        backgroundColor = Color(0xFF148A08),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Features124/v4/b2/3b/7d/b23b7d36-c61d-d207-a3bd-670466ed9dd1/dj.fcmnjull.jpg/300x300bb.jpg",
        tag = "Pop"
    ),
    MimikyuBrowseItem(
        title = "Country",
        backgroundColor = Color(0xFFBA5D07),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Features114/v4/36/d0/3b/36d03b1b-6284-faed-2379-4f1d7baefde2/dj.hzkfsdta.jpg/300x300bb.jpg",
        tag = "Country"
    ),
    MimikyuBrowseItem(
        title = "Hip-Hop",
        backgroundColor = Color(0xFFBC5900),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music114/v4/bc/f1/82/bcf182d9-7395-fe2c-3540-df22e4eefab7/eb7c7928-3355-4ae6-b5a0-fbe3d58620ba.jpg/300x300bb.jpg",
        tag = "Hip-Hop"
    ),
    MimikyuBrowseItem(
        title = "Rock",
        backgroundColor = Color(0xFFE91429),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/ad/d0/ed/add0ed5b-bf41-312c-4c32-614f9c76e71b/723277880365.jpg/300x300bb.jpg",
        tag = "Rock"
    ),
    MimikyuBrowseItem(
        title = "Latin",
        backgroundColor = Color(0xFFE1118C),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music128/v4/8a/7c/34/8a7c34e9-982d-2447-dc16-c7078d9581b0/LatinHits_ReggaetonEdition_iTunes.jpg/300x300bb.jpg",
        tag = "Latin"
    ),
    MimikyuBrowseItem(
        title = "Dance /\nElectronic",
        backgroundColor = Color(0xFF006450),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/2e/d5/43/2ed54360-4339-e96c-46a9-0b273e906762/886446681832.jpg/300x300bb.jpg",
        tag = "Dance"
    ),
    MimikyuBrowseItem(
        title = "Indie",
        backgroundColor = Color(0xFF608108),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/65/e3/e7/65e3e740-b69f-f5cb-f2e6-7dedb5265ac9/19UMGIM96748.rgb.jpg/300x300bb.jpg",
        tag = "Indie"
    ),
    MimikyuBrowseItem(
        title = "R&B",
        backgroundColor = Color(0xFFDC148C),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/7c/92/dd/7c92dd78-928e-1d77-6be9-e904c6912955/06UMGIM12221.rgb.jpg/300x300bb.jpg",
        tag = "R&B"
    ),
    MimikyuBrowseItem(
        title = "K-Pop",
        backgroundColor = Color(0xFF274288),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/42/62/49/426249e9-a964-8276-f285-226df5b52c1b/196871325431.jpg/300x300bb.jpg",
        tag = "K-Pop"
    ),
    MimikyuBrowseItem(
        title = "Chill",
        backgroundColor = Color(0xFF4D6B78),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/77/74/ec/7774ecea-316e-65a8-5f58-02e824eca66e/07c11e2b-715f-4348-be10-739607d8da57.jpg/300x300bb.jpg",
        tag = "Chill"
    ),
    MimikyuBrowseItem(
        title = "Mood",
        backgroundColor = Color(0xFF8D67AB),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/2a/1e/d6/2a1ed66f-de13-f295-6d2c-e6f7d6d76c5b/5063501799199_cover.jpg/300x300bb.jpg",
        tag = "Mood"
    ),
    MimikyuBrowseItem(
        title = "Party",
        backgroundColor = Color(0xFF537AA1),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music128/v4/50/0c/e2/500ce2f2-e944-0732-f579-8fb2ab04237c/842474167755.jpg/300x300bb.jpg",
        tag = "Party"
    ),
    MimikyuBrowseItem(
        title = "Focus",
        backgroundColor = Color(0xFF503750),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/ce/55/41/ce55411c-a4fd-6931-55b8-2605c9354e6e/cover.jpg/300x300bb.jpg",
        tag = "Focus"
    ),
    MimikyuBrowseItem(
        title = "Sleep",
        backgroundColor = Color(0xFF1E3264),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music114/v4/a9/32/cf/a932cfca-b28e-6cf3-e419-edc47f82c52f/NIRS-023_Mellow_Sleep_Deep_Sleep_AMBIENT_COVER.jpg/300x300bb.jpg",
        tag = "Sleep"
    ),
    MimikyuBrowseItem(
        title = "Workout",
        backgroundColor = Color(0xFF777777),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music/fd/5f/3d/mzi.chvkmdti.tif/300x300bb.jpg",
        tag = "Workout"
    ),
    MimikyuBrowseItem(
        title = "Classical",
        backgroundColor = Color(0xFF7D4B32),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music/05/6f/54/mzi.gtqpqaiu.jpg/300x300bb.jpg",
        tag = "Classical"
    ),
    MimikyuBrowseItem(
        title = "Jazz",
        backgroundColor = Color(0xFF1A6B52),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music113/v4/11/47/60/11476044-976d-ce6e-cdac-328e692b1a5b/859769587341_cover.jpg/300x300bb.jpg",
        tag = "Jazz"
    ),
    MimikyuBrowseItem(
        title = "Soul",
        backgroundColor = Color(0xFFD86C00),
        artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music114/v4/2f/93/03/2f93036d-29c6-80d0-0d34-1daf658e4b9b/084247977226.jpg/300x300bb.jpg",
        tag = "Soul"
    )
)

private val tagFallbackColors = listOf(
    Color(0xFF1E3264),
    Color(0xFF8D67AB),
    Color(0xFF148A08),
    Color(0xFFE13300),
    Color(0xFF477D95),
    Color(0xFFE8115B),
    Color(0xFF537AA1),
    Color(0xFF7D4B32),
    Color(0xFF1A6B52),
    Color(0xFFE91429),
)

@Composable
internal fun SearchDiscoveryContent(
    state: DiscoveryUiState,
    featuredTrack: Track?,
    onHotSearchClick: (String) -> Unit,
    onPlayFeatured: () -> Unit,
    onPlaylistTagClick: (String) -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        DiscoveryUiState.Loading -> {
            Column(modifier = Modifier.fillMaxSize().padding(top = MimikyuSpacing.md)) {
                DiscoverySectionSkeleton()
                Spacer(Modifier.height(MimikyuSpacing.lg))
                DiscoverySectionSkeleton()
            }
        }
        is DiscoveryUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ErrorState(message = state.message, onRetry = onRetry)
            }
        }
        is DiscoveryUiState.Success -> {
            // Build dynamically combined genres with any remote tags not already present
            val extraGenreItems = state.playlistTags
                .filter { tag -> genreItems.none { it.title.equals(tag.name, ignoreCase = true) } }
                .mapIndexed { index, tag ->
                    MimikyuBrowseItem(
                        title = tag.name,
                        backgroundColor = tagFallbackColors[index % tagFallbackColors.size],
                        artworkUrl = tag.coverUrl,
                        tag = tag.name
                    )
                }
            val allGenres = genreItems + extraGenreItems

            AdaptiveContentWidth {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = MimikyuSpacing.md,
                        end = MimikyuSpacing.md,
                        top = MimikyuSpacing.xs,
                        bottom = LocalBottomOverlayInset.current + MimikyuSpacing.md
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(key = "browse_all_header") {
                        Text(
                            text = "Browse all",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 14.dp, bottom = 12.dp)
                        )
                    }

                    item(key = "discover_header") {
                        Text(
                            text = "Discover",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    itemsIndexed(discoverItems.chunked(2), key = { index, _ -> "discover_row_$index" }) { _, pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MimikyuBrowseCard(
                                item = pair[0],
                                onClick = { onPlaylistTagClick(pair[0].tag) },
                                modifier = Modifier.weight(1f)
                            )
                            if (pair.size > 1) {
                                MimikyuBrowseCard(
                                    item = pair[1],
                                    onClick = { onPlaylistTagClick(pair[1].tag) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    item(key = "genres_header") {
                        Text(
                            text = "Genres",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                        )
                    }

                    itemsIndexed(allGenres.chunked(2), key = { index, _ -> "genre_row_$index" }) { _, pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MimikyuBrowseCard(
                                item = pair[0],
                                onClick = { onPlaylistTagClick(pair[0].tag) },
                                modifier = Modifier.weight(1f)
                            )
                            if (pair.size > 1) {
                                MimikyuBrowseCard(
                                    item = pair[1],
                                    onClick = { onPlaylistTagClick(pair[1].tag) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MimikyuBrowseCard(
    item: MimikyuBrowseItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(item.backgroundColor)
            .pressable(MimikyuPress.Card, onClick = onClick)
    ) {
        // Rotated album artwork at bottom-right corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 18.dp, y = 4.dp)
                .graphicsLayer {
                    rotationZ = 25f
                }
                .size(66.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF202020))
        ) {
            if (item.artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = item.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Title on top-left
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 19.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp, end = 48.dp)
        )
    }
}
