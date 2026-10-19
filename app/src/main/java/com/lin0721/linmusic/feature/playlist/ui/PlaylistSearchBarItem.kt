package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.MimikyuIcons
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.RadiusCompact
import com.lin0721.linmusic.core.ui.theme.darken
import kotlinx.coroutines.delay

private val SearchBarHeight = 40.dp

// ────────────────────────────────────────────────────────────────────────────
// Search Bar Item with Keyboard Auto-Focus and Spotify-style input
// ────────────────────────────────────────────────────────────────────────────
@Composable
fun SearchBarItem(
    query: String,
    onQueryChange: (String) -> Unit,
    topPadding: Dp,
    backgroundColor: Color,
    isOpen: Boolean = false,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isOpen) {
        if (isOpen) {
            delay(120)
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {}
        } else {
            keyboardController?.hide()
        }
    }

    val animatedBackground by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(800),
        label = "search_bar_color"
    )
    val fillColor = remember(animatedBackground) { animatedBackground.darken(0.35f) }
    Column(modifier = modifier.fillMaxWidth().background(fillColor)) {
        // Space for system status bar / top overlay
        Spacer(modifier = Modifier.height(topPadding))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(MimikyuIcons.ArrowLeft),
                contentDescription = "Close search",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .pressable(MimikyuPress.Icon) {
                        onQueryChange("")
                        onClose()
                    }
            )

            Spacer(Modifier.width(10.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(SearchBarHeight)
                    .clip(RoundedCornerShape(RadiusCompact))
                    .background(Color.White.copy(alpha = 0.24f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value         = query,
                    onValueChange = onQueryChange,
                    singleLine    = true,
                    textStyle     = androidx.compose.ui.text.TextStyle(
                        color    = Color.White,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(Color.White),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Find in playlist",
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 14.sp
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(20.dp)
                            .pressable(MimikyuPress.Icon) { onQueryChange("") }
                    )
                    Spacer(Modifier.width(8.dp))
                } else {
                    Spacer(Modifier.width(12.dp))
                }
            }
        }
    }
}
