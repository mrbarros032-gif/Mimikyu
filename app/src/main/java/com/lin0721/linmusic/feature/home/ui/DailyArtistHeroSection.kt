package com.lin0721.linmusic.feature.home.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.R
import com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry
import com.lin0721.linmusic.core.ui.theme.RobotoFontFamily

data class HeroArtistItem(
    val name: String,
    val albumName: String,
    val ytmBrowseId: String,
    val drawableResId: Int
)

val HERO_ARTISTS: List<HeroArtistItem> = listOf(
    HeroArtistItem(
        name = "Michael Jackson",
        albumName = "Thriller",
        ytmBrowseId = "MPREb_dqWTncCjkSp",
        drawableResId = R.drawable.hero_michael_jackson
    ),
    HeroArtistItem(
        name = "Kanye West",
        albumName = "Graduation",
        ytmBrowseId = "MPREb_KCNeTnK02S7",
        drawableResId = R.drawable.hero_kanye_west
    ),
    HeroArtistItem(
        name = "Tame Impala",
        albumName = "Currents",
        ytmBrowseId = "MPREb_AjwAqo5hI0U",
        drawableResId = R.drawable.hero_tame_impala
    ),
    HeroArtistItem(
        name = "The Weeknd",
        albumName = "Starboy",
        ytmBrowseId = "MPREb_FWIMEPTHFsY",
        drawableResId = R.drawable.hero_the_weeknd
    ),
    HeroArtistItem(
        name = "BTS",
        albumName = "ARIRANG",
        ytmBrowseId = "MPREb_eSR1QCwkrKw",
        drawableResId = R.drawable.hero_bts
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DailyArtistHeroSection(
    onOpenAlbum: (Long) -> Unit,
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val heroHeight = (screenHeight * 0.46f).coerceAtLeast(380.dp)
    val pagerState = rememberPagerState(pageCount = { HERO_ARTISTS.size })

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight)
    ) {
        // Swipeable 5-artist Hero Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val artist = HERO_ARTISTS[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        val albumId = YouTubeIdRegistry.registerAlbum(artist.ytmBrowseId)
                        onOpenAlbum(albumId)
                    }
            ) {
                // 1. Hero Image
                Image(
                    painter = painterResource(id = artist.drawableResId),
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                )

                // 2. AMOLED Dark Overlays
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Black.copy(alpha = 0.55f),
                                0.18f to Color.Black.copy(alpha = 0.20f),
                                0.35f to Color.Transparent,
                                0.55f to Color.Black.copy(alpha = 0.30f),
                                0.72f to Color.Black.copy(alpha = 0.70f),
                                0.88f to Color.Black.copy(alpha = 0.94f),
                                1.0f to Color.Black
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0.0f to Color.Black.copy(alpha = 0.40f),
                                0.65f to Color.Transparent
                            )
                        )
                )

                // 3. Artist Name Only (NO album name, per user instruction)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 36.dp, start = 24.dp, end = 24.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = artist.name,
                        color = Color.White,
                        fontSize = 38.sp,
                        fontFamily = RobotoFontFamily,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        lineHeight = 42.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 4. Dot Indicators at bottom (filter pills removed so hero is clean upon opening)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(HERO_ARTISTS.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .width(if (isSelected) 18.dp else 5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isSelected) Color.White else Color.White.copy(alpha = 0.35f)
                        )
                )
            }
        }
    }
}
