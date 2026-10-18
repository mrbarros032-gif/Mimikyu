package com.lin0721.linmusic.feature.player.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreHoriz
import com.lin0721.linmusic.core.ui.theme.MimikyuIcons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lin0721.linmusic.R
import com.lin0721.linmusic.core.player.domain.LyricLine
import kotlinx.coroutines.launch

enum class ShareCardTab {
    TRACK,
    LYRICS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MimikyuShareSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    mediaId: String,
    title: String,
    artist: String,
    coverUrl: String,
    currentPositionMs: Long,
    lyrics: List<LyricLine> = emptyList(),
    currentLyricIndex: Int = 0,
    baseColor: Color = Color(0xFF1E3264),
    initialTab: ShareCardTab = ShareCardTab.TRACK
) {
    if (!visible) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val hasLyrics = remember(lyrics) { lyrics.any { !it.isGap && it.text.isNotBlank() } }
    var selectedTab by remember(initialTab, hasLyrics) {
        mutableStateOf(if (initialTab == ShareCardTab.LYRICS && hasLyrics) ShareCardTab.LYRICS else ShareCardTab.TRACK)
    }

    val currentPositionSec = remember(currentPositionMs) { (currentPositionMs / 1000L).coerceAtLeast(0L) }
    var shareTimestamp by remember { mutableStateOf(false) }

    // Lyric selection
    var selectedLyricIdx by remember(currentLyricIndex, lyrics) {
        val nonGapIdx = if (currentLyricIndex in lyrics.indices && !lyrics[currentLyricIndex].isGap && lyrics[currentLyricIndex].text.isNotBlank()) {
            currentLyricIndex
        } else {
            lyrics.indexOfFirst { !it.isGap && it.text.isNotBlank() }.coerceAtLeast(0)
        }
        mutableIntStateOf(nonGapIdx)
    }

    val selectedLyricText = remember(selectedLyricIdx, lyrics) {
        lyrics.getOrNull(selectedLyricIdx)?.text?.trim().orEmpty().ifBlank { "..." }
    }

    var isProcessing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Share",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF282828))
                        .clickable {
                            scope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(MimikyuIcons.Close),
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 2. Tab Selector (Track vs Lyrics) - Mimikyu Style Pills
            if (hasLyrics) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MimikyuTabPill(
                        label = "Song",
                        selected = selectedTab == ShareCardTab.TRACK,
                        onClick = { selectedTab = ShareCardTab.TRACK }
                    )
                    MimikyuTabPill(
                        label = "Lyrics",
                        selected = selectedTab == ShareCardTab.LYRICS,
                        onClick = { selectedTab = ShareCardTab.LYRICS }
                    )
                }
            }

            // 3. Mimikyu Live Share Card Preview (9:16 Aspect Ratio)
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .height(340.dp)
                    .aspectRatio(9f / 16f)
                    .shadow(24.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                baseColor,
                                Color(0xFF101014)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (selectedTab == ShareCardTab.TRACK) {
                    // === TRACK CARD PREVIEW ===
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))

                        // Artwork
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(130.dp)
                                .shadow(12.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF282828))
                        )

                        // Title & Artist & Timestamp badge
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            if (shareTimestamp && currentPositionSec > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(bottom = 8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(MimikyuIcons.Play),
                                        contentDescription = null,
                                        tint = Color(0xFF1ED760),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = MimikyuShareEngine.formatTimestamp(currentPositionSec),
                                        color = Color(0xFF1ED760),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = artist,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Bottom Soundwave Code & Branding
                        MimikyuSoundwaveCode()
                    }
                } else {
                    // === LYRICS CARD PREVIEW ===
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            // Large quotation mark
                            Text(
                                text = "“",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 38.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold
                            )

                            // Selected lyric text
                            Text(
                                text = selectedLyricText,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // Bottom Attribution Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF282828))
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = artist,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            MimikyuSoundwaveMini()
                        }
                    }
                }

                // Processing overlay
                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1ED760),
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }
            }

            // 4. Timestamp Option Row (Mimikyu "Share from mm:ss" toggle)
            if (currentPositionSec > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E))
                        .clickable { shareTimestamp = !shareTimestamp }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(MimikyuIcons.Play),
                            contentDescription = null,
                            tint = if (shareTimestamp) Color(0xFF1ED760) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Share from ${MimikyuShareEngine.formatTimestamp(currentPositionSec)}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Switch(
                        checked = shareTimestamp,
                        onCheckedChange = { shareTimestamp = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF1ED760),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color(0xFF333333)
                        )
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 5. Off-Platform Share Destinations (Smart Sorting Horizontal Row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // A. Copy Link
                MimikyuShareDestinationItem(
                    label = "Copy link",
                    iconRes = R.drawable.ic_share_copy_link,
                    bgColor = Color(0xFF242424),
                    tintColor = Color.White,
                    onClick = {
                        MimikyuShareEngine.copyLink(
                            context = context,
                            mediaId = mediaId,
                            timestampSec = if (shareTimestamp) currentPositionSec else null,
                            title = title,
                            artist = artist
                        )
                    }
                )

                // B. Stories (Instagram)
                MimikyuShareDestinationItem(
                    label = "Stories",
                    iconRes = R.drawable.ic_share_instagram,
                    gradient = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF833AB4),
                            Color(0xFFFD1D1D),
                            Color(0xFFF77737)
                        )
                    ),
                    tintColor = Color.White,
                    onClick = {
                        scope.launch {
                            isProcessing = true
                            val ts = if (shareTimestamp) currentPositionSec else null
                            val lText = if (selectedTab == ShareCardTab.LYRICS) selectedLyricText else null
                            val (_, uri) = MimikyuShareEngine.renderShareCard(
                                context = context,
                                title = title,
                                artist = artist,
                                coverUrl = coverUrl,
                                timestampSec = ts,
                                lyrics = lText,
                                gradientStart = baseColor.toArgb()
                            )
                            isProcessing = false
                            MimikyuShareEngine.shareToInstagramStories(context, uri)
                        }
                    }
                )

                // C. WhatsApp
                MimikyuShareDestinationItem(
                    label = "WhatsApp",
                    iconRes = R.drawable.ic_share_whatsapp,
                    bgColor = Color(0xFF25D366),
                    tintColor = Color.White,
                    onClick = {
                        scope.launch {
                            isProcessing = true
                            val ts = if (shareTimestamp) currentPositionSec else null
                            val lText = if (selectedTab == ShareCardTab.LYRICS) selectedLyricText else null
                            val shareUrl = MimikyuShareEngine.buildShareUrl(mediaId, ts)
                            val shareText = if (lText != null) {
                                "“$lText”\n$title - $artist\n$shareUrl"
                            } else {
                                "$title - $artist\n$shareUrl"
                            }
                            val (_, uri) = MimikyuShareEngine.renderShareCard(
                                context = context,
                                title = title,
                                artist = artist,
                                coverUrl = coverUrl,
                                timestampSec = ts,
                                lyrics = lText,
                                gradientStart = baseColor.toArgb()
                            )
                            isProcessing = false
                            MimikyuShareEngine.shareToWhatsApp(context, shareText, uri)
                        }
                    }
                )

                // D. Snapchat
                MimikyuShareDestinationItem(
                    label = "Snapchat",
                    iconRes = R.drawable.ic_share_snapchat,
                    bgColor = Color(0xFFFFFC00),
                    tintColor = Color.Black,
                    onClick = {
                        scope.launch {
                            isProcessing = true
                            val ts = if (shareTimestamp) currentPositionSec else null
                            val lText = if (selectedTab == ShareCardTab.LYRICS) selectedLyricText else null
                            val (_, uri) = MimikyuShareEngine.renderShareCard(
                                context = context,
                                title = title,
                                artist = artist,
                                coverUrl = coverUrl,
                                timestampSec = ts,
                                lyrics = lText,
                                gradientStart = baseColor.toArgb()
                            )
                            isProcessing = false
                            MimikyuShareEngine.shareToSnapchat(context, uri)
                        }
                    }
                )

                // E. X (Twitter)
                MimikyuShareDestinationItem(
                    label = "X",
                    iconRes = R.drawable.ic_share_x,
                    bgColor = Color(0xFF0F1419),
                    tintColor = Color.White,
                    border = 1.dp to Color.White.copy(alpha = 0.2f),
                    onClick = {
                        val ts = if (shareTimestamp) currentPositionSec else null
                        val lText = if (selectedTab == ShareCardTab.LYRICS) selectedLyricText else null
                        val shareUrl = MimikyuShareEngine.buildShareUrl(mediaId, ts)
                        val tweetText = if (lText != null) "“$lText” — $title by $artist" else "Listening to $title by $artist on Mimikyu"
                        MimikyuShareEngine.shareToTwitter(context, tweetText, shareUrl)
                    }
                )

                // F. Save Image
                MimikyuShareDestinationItem(
                    label = "Save image",
                    iconRes = MimikyuIcons.Download,
                    bgColor = Color(0xFF242424),
                    tintColor = Color.White,
                    onClick = {
                        scope.launch {
                            isProcessing = true
                            val ts = if (shareTimestamp) currentPositionSec else null
                            val lText = if (selectedTab == ShareCardTab.LYRICS) selectedLyricText else null
                            val (bitmap, _) = MimikyuShareEngine.renderShareCard(
                                context = context,
                                title = title,
                                artist = artist,
                                coverUrl = coverUrl,
                                timestampSec = ts,
                                lyrics = lText,
                                gradientStart = baseColor.toArgb()
                            )
                            isProcessing = false
                            MimikyuShareEngine.saveImageToGallery(context, bitmap)
                        }
                    }
                )

                // G. More (System Chooser)
                MimikyuShareDestinationItem(
                    label = "More",
                    iconRes = MimikyuIcons.More,
                    bgColor = Color(0xFF242424),
                    tintColor = Color.White,
                    onClick = {
                        scope.launch {
                            isProcessing = true
                            val ts = if (shareTimestamp) currentPositionSec else null
                            val lText = if (selectedTab == ShareCardTab.LYRICS) selectedLyricText else null
                            val shareUrl = MimikyuShareEngine.buildShareUrl(mediaId, ts)
                            val shareText = if (lText != null) {
                                "“$lText”\n$title - $artist\n$shareUrl"
                            } else {
                                "$title - $artist\n$shareUrl"
                            }
                            val (_, uri) = MimikyuShareEngine.renderShareCard(
                                context = context,
                                title = title,
                                artist = artist,
                                coverUrl = coverUrl,
                                timestampSec = ts,
                                lyrics = lText,
                                gradientStart = baseColor.toArgb()
                            )
                            isProcessing = false
                            MimikyuShareEngine.shareSystem(context, shareText, uri)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MimikyuTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Color.White else Color(0xFF282828))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MimikyuSoundwaveCode() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        // Soundwave rhythmic bars
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            modifier = Modifier.height(16.dp)
        ) {
            val barHeights = listOf(
                5.dp, 10.dp, 7.dp, 14.dp, 11.dp, 16.dp, 9.dp, 13.dp, 15.dp, 8.dp,
                12.dp, 16.dp, 10.dp, 7.dp, 14.dp, 9.dp, 15.dp, 11.dp, 6.dp, 13.dp,
                8.dp, 14.dp, 10.dp, 5.dp
            )
            barHeights.forEach { h ->
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(h)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.85f))
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "MIMIKYU",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 8.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MimikyuSoundwaveMini() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height(12.dp)
    ) {
        val bars = listOf(4.dp, 8.dp, 12.dp, 7.dp, 11.dp, 5.dp, 9.dp)
        bars.forEach { h ->
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(h)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.7f))
            )
        }
    }
}

@Composable
private fun MimikyuShareDestinationItem(
    label: String,
    iconRes: Int? = null,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    bgColor: Color? = null,
    gradient: Brush? = null,
    tintColor: Color = Color.White,
    border: Pair<androidx.compose.ui.unit.Dp, Color>? = null,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(62.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .then(
                    when {
                        gradient != null -> Modifier.background(gradient)
                        bgColor != null -> Modifier.background(bgColor)
                        else -> Modifier.background(Color(0xFF242424))
                    }
                )
                .then(
                    if (border != null) Modifier.border(border.first, border.second, CircleShape) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = tintColor,
                    modifier = Modifier.size(24.dp)
                )
            } else if (iconVector != null) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = label,
                    tint = tintColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}
