package com.lin0721.linmusic.feature.home.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.lin0721.linmusic.core.ui.theme.MimikyuIcons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.lin0721.linmusic.LocalBottomOverlayInset
import com.lin0721.linmusic.core.songlike.LikedSong
import com.lin0721.linmusic.core.ui.components.AlbumArtImage
import com.lin0721.linmusic.core.ui.components.CoverPlaceholder
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.RadiusCompact
import com.lin0721.linmusic.core.ui.theme.RobotoFontFamily
import com.lin0721.linmusic.core.ui.theme.TextGray

private val EdgePadding = 20.dp
private val HeaderHeight = 152.dp

/**
 * Liked Songs screen content backed by Firestore.
 * Requirements:
 * - Render from repository's list
 * - Use key = { it.videoId }
 * - Skip any document missing videoId or title instead of crashing
 * - Show an empty state if none
 * - Show a signed-out state if there is no uid
 */
@Composable
fun LikedSongsContent(
    likedSongs: List<LikedSong>,
    uid: String?,
    onPlaySongAt: (Int) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onToggleLike: (LikedSong) -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    // Skip any document missing videoId or title instead of crashing, and deduplicate
    val validSongs = remember(likedSongs) {
        likedSongs.filter { it.videoId.isNotBlank() && it.title.isNotBlank() }.distinctBy { it.videoId }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalBottomOverlayInset.current + 24.dp)
    ) {
        if (onBack != null) {
            item(key = "top_nav_bar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EdgePadding, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(MimikyuIcons.ArrowLeft),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Liked Songs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        // Hero Header Card
        item(key = "liked_header") {
            LikedSongsHeader(
                trackCount = validSongs.size,
                onPlay = onPlayAll,
                onShuffle = onShuffle,
                enabled = validSongs.isNotEmpty()
            )
        }

        when {
            // Render from repository's list with key = { it.videoId } if songs exist
            validSongs.isNotEmpty() -> {
                item(key = "section_title") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = EdgePadding, end = EdgePadding, top = 18.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "Your Collection",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontFamily = RobotoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${validSongs.size} songs",
                            color = TextGray,
                            fontSize = 12.sp,
                            fontFamily = RobotoFontFamily,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                itemsIndexed(
                    items = validSongs,
                    key = { index, song -> "${song.videoId}_$index" }
                ) { index, song ->
                    LikedSongRow(
                        index = index,
                        song = song,
                        onPlay = { onPlaySongAt(index) },
                        onToggleLike = { onToggleLike(song) }
                    )
                }
            }
            uid == null -> {
                item(key = "signed_out_state") {
                    LikedSongsSignedOutState(onSignInClick = onSignInClick)
                }
            }
            else -> {
                item(key = "empty_state") {
                    LikedSongsEmptyState()
                }
            }
        }
    }
}

@Composable
private fun LikedSongsHeader(
    trackCount: Int,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    enabled: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = EdgePadding, vertical = 14.dp)
            .height(HeaderHeight)
            .clip(RoundedCornerShape(RadiusCompact))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF450AF5),
                        Color(0xFF2E136D),
                        Color(0xFF121212)
                    )
                )
            )
    ) {
        // Watermark text in background
        Text(
            text = "LIKED",
            color = Color.White.copy(alpha = 0.12f),
            fontSize = 36.sp,
            fontFamily = RobotoFontFamily,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 16.dp)
        )

        // Left info column
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .fillMaxWidth(0.65f)
        ) {
            Text(
                text = "Liked Songs",
                color = Color.White,
                fontSize = 24.sp,
                fontFamily = RobotoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = if (trackCount > 0) {
                "$trackCount favorite tracks"
            } else {
                "Your saved favorites"
            }
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 12.sp,
                fontFamily = RobotoFontFamily,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Action controls (Shuffle & Play)
        if (enabled) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Shuffle button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .pressable(MimikyuPress.Icon) { onShuffle() }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(MimikyuIcons.Shuffle),
                        contentDescription = "Shuffle",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Play All button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .pressable(MimikyuPress.Icon) { onPlay() }
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(MimikyuIcons.Play),
                        contentDescription = "Play All",
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LikedSongRow(
    index: Int,
    song: LikedSong,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(horizontal = EdgePadding, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number index
        Text(
            text = "${index + 1}",
            color = TextGray,
            fontSize = 12.sp,
            modifier = Modifier.width(22.dp)
        )

        // Album cover
        val songId = remember(song.videoId) {
            com.lin0721.linmusic.core.player.innertube.YouTubeIdRegistry.register(
                videoId = song.videoId,
                title = song.title,
                artist = song.artist,
                coverUrl = song.thumbnailUrl
            )
        }
        AlbumArtImage(
            url = song.thumbnailUrl,
            songId = songId,
            title = song.title,
            artist = song.artist,
            contentDescription = song.title,
            shape = RoundedCornerShape(8.dp),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(42.dp)
        )

        // Title and artist
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = song.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontFamily = RobotoFontFamily,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val artistText = song.artist.ifBlank { song.album.orEmpty() }
            Text(
                text = artistText,
                color = TextGray,
                fontSize = 12.sp,
                fontFamily = RobotoFontFamily,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Heart like toggle button
        IconButton(
            onClick = onToggleLike,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                painter = painterResource(MimikyuIcons.HeartFilled),
                contentDescription = "Unlike",
                tint = Color(0xFF1DB954),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun LikedSongsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(MimikyuIcons.Heart),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "No Liked Songs Yet",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Tap the heart icon on any song in the home feed or player to add it to your Liked Songs collection.",
            color = TextGray,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LikedSongsSignedOutState(onSignInClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(MimikyuIcons.Locked),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Sign in to see your Liked Songs",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Log in with your Google or Mimikyu account to sync and access your favorite tracks anywhere.",
            color = TextGray,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onSignInClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE91E63),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Sign In",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}
