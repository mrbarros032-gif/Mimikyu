package com.lin0721.linmusic.feature.artist.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.provider.Settings
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import com.lin0721.linmusic.core.ui.theme.ScreenSlideDurationMs
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.lin0721.linmusic.core.ui.components.MimikyuIconButton
import com.lin0721.linmusic.core.ui.components.MimikyuTextButton
import com.lin0721.linmusic.core.ui.components.MimikyuButton
import com.lin0721.linmusic.LocalBottomOverlayInset
import com.lin0721.linmusic.core.ui.theme.BackgroundDark
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.interaction.pressScale
import com.lin0721.linmusic.core.ui.interaction.pressHighlight
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.components.CoverPlaceholder
import com.lin0721.linmusic.core.ui.theme.RadiusCompact
import com.lin0721.linmusic.core.model.ArtistMv
import com.lin0721.linmusic.core.ui.components.ToastManager
import com.lin0721.linmusic.core.ui.theme.rememberMimikyuOrientationClass
import com.lin0721.linmusic.core.ui.theme.rememberMimikyuWindowSizeClass
import com.lin0721.linmusic.core.ui.theme.MimikyuOrientationClass
import com.lin0721.linmusic.core.ui.theme.MimikyuWindowSizeClass
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.androidx.compose.koinViewModel

private const val TAG = "ArtistMvPlayerScreen"
private val QUALITY_OPTIONS = listOf(1080, 720, 480, 240)

// （/） of
private data class GestureFeedback(val icon: ImageVector, val fraction: Float)

// MV Play：YouTube ——， of （Artists/Play/SaveShare//ArtistsMoreMV）；
// Play（/Custom media3-ui）
@Composable
fun ArtistMvPlayerScreen(
    mvId: Long,
    mvName: String,
    viewModel: ArtistMvPlayerViewModel = koinViewModel(),
    onBack: () -> Unit,
    onArtistClick: (Long) -> Unit,
    onMvClick: (Long, String) -> Unit,
    onFullscreenChanged: (Boolean) -> Unit,
    onCommentsVisibilityChanged: (Boolean) -> Unit = {},
    onNavigateToProfile: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mvDetail by viewModel.mvDetail.collectAsStateWithLifecycle()
    val relatedMvs by viewModel.relatedMvs.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val artistAvatar by viewModel.artistAvatar.collectAsStateWithLifecycle()
    val fansCount by viewModel.fansCount.collectAsStateWithLifecycle()
    val isArtistFollowed by viewModel.isArtistFollowed.collectAsStateWithLifecycle()

    var quality by remember { mutableStateOf(1080) }

    LaunchedEffect(mvId) {
        quality = 1080
        viewModel.loadMvUrl(mvId, 1080)
        viewModel.loadMvDetail(mvId)
    }

    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collect { ToastManager.showToast(it) }
    }

    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current

    // Expanded ： and ，
    // （“” of ， LaunchedEffect(configuration.orientation)）。
    // of  and ，，
    val windowSizeClass = rememberMimikyuWindowSizeClass()
    val orientationClass = rememberMimikyuOrientationClass()
    val useLandscapeSplitLayout = windowSizeClass == MimikyuWindowSizeClass.Expanded &&
            orientationClass == MimikyuOrientationClass.Landscape

    var isFullscreen by remember {
        mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && !useLandscapeSplitLayout)
    }

    // "Auto Rotate"：，；Close
    val systemAutoRotateEnabled = remember {
        runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        }.getOrDefault(false)
    }

    var brightness by remember {
        mutableStateOf(
            (activity?.window?.attributes?.screenBrightness ?: -1f).let { current ->
                if (current in 0f..1f) current
                else runCatching {
                    Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
                }.getOrDefault(0.5f)
            }
        )
    }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volumeFraction by remember {
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
    }

    // ：Auto Rotate，，； +  +  +
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation
        val originalBrightness = activity?.window?.attributes?.screenBrightness
        activity?.requestedOrientation = if (systemAutoRotateEnabled) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.window?.let { window ->
                val lp = window.attributes
                lp.screenBrightness = originalBrightness ?: -1f
                window.attributes = lp
            }
            val insetsController = activity?.window?.let { WindowInsetsControllerCompat(it, view) }
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            onFullscreenChanged(false)
            onCommentsVisibilityChanged(false)
        }
    }



    // -> （ Expanded ，）
    LaunchedEffect(configuration.orientation, useLandscapeSplitLayout) {
        if (!useLandscapeSplitLayout) {
            isFullscreen = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
    }

    // -> /、/、"Release"
    LaunchedEffect(isFullscreen) {

        onFullscreenChanged(isFullscreen)
        val insetsController = activity?.window?.let { WindowInsetsControllerCompat(it, view) }
        if (isFullscreen) {
            insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
        // Auto Rotate -> Release，；
        // -> Current，
        activity?.requestedOrientation = when {
            systemAutoRotateEnabled -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            isFullscreen -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    fun enterFullscreen() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
    fun exitFullscreen() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }

    BackHandler(enabled = isFullscreen) { exitFullscreen() }

    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var bufferedPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var videoAspectRatio by remember { mutableStateOf(16f / 9f) }
    var isBuffering by remember { mutableStateOf(true) }
    var isEnded by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var seekFeedback by remember { mutableStateOf<String?>(null) }
    var gestureFeedback by remember { mutableStateOf<GestureFeedback?>(null) }

    // /Pause， MV Play
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspectRatio = (videoSize.width * videoSize.pixelWidthHeightRatio) / videoSize.height
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                isEnded = state == Player.STATE_ENDED
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                AppLogger.e(TAG, "MV playback error: mvId=$mvId", error)
                playbackError = "Video playback error, please retry"
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Play/（，，Play of ）
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
            bufferedPosition = exoPlayer.bufferedPosition.coerceAtLeast(0L)
            delay(500)
        }
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is MvPlayerUiState.Success) {
            val newUri = state.videoUrl
            if (exoPlayer.currentMediaItem?.localConfiguration?.uri.toString() != newUri) {
                val resumeAt = exoPlayer.currentPosition.coerceAtLeast(0L)
                exoPlayer.setMediaItem(MediaItem.fromUri(newUri))
                exoPlayer.prepare()
                if (resumeAt > 0) exoPlayer.seekTo(resumeAt)
                exoPlayer.playWhenReady = true
                playbackError = null
            }
        }
    }

    LaunchedEffect(isEnded) {
        if (isEnded) controlsVisible = true
    }

    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3500)
            controlsVisible = false
        }
    }

    LaunchedEffect(seekFeedback) {
        if (seekFeedback != null) {
            delay(600)
            seekFeedback = null
        }
    }

    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback != null) {
            delay(800)
            gestureFeedback = null
        }
    }

    fun seekBy(deltaMs: Long) {
        val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, duration.coerceAtLeast(0L))
        exoPlayer.seekTo(target)
        currentPosition = target
        seekFeedback = if (deltaMs < 0) "-10s" else "+10s"
    }

    // ：(//) + Play + ， and
    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Composable
    fun VideoArea(modifier: Modifier) {
        Box(
            modifier = modifier
                .background(Color.Black)
                .pointerInput(Unit) {
                    val touchSlop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val isLeftZone = down.position.x < size.width / 2f
                        var dragging = false
                        var lastY = down.position.y
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            if (!dragging && kotlin.math.abs(change.position.y - down.position.y) > touchSlop) {
                                dragging = true
                            }
                            if (dragging) {
                                change.consume()
                                val deltaFraction = (lastY - change.position.y) / size.height.toFloat()
                                if (isLeftZone) {
                                    brightness = (brightness + deltaFraction).coerceIn(0f, 1f)
                                    activity?.window?.let { window ->
                                        val lp = window.attributes
                                        lp.screenBrightness = brightness
                                        window.attributes = lp
                                    }
                                    gestureFeedback = GestureFeedback(Icons.Default.WbSunny, brightness)
                                } else {
                                    volumeFraction = (volumeFraction + deltaFraction).coerceIn(0f, 1f)
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        (volumeFraction * maxVolume).toInt(),
                                        0
                                    )
                                    gestureFeedback = GestureFeedback(Icons.AutoMirrored.Filled.VolumeUp, volumeFraction)
                                }
                            }
                            lastY = change.position.y
                        }
                        if (!dragging) {
                            // ：，/
                            val secondDown = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                                awaitFirstDown(requireUnconsumed = false)
                            }
                            if (secondDown != null) {
                                do {
                                    val event2 = awaitPointerEvent()
                                    val change2 = event2.changes.firstOrNull { it.id == secondDown.id } ?: break
                                } while (change2.pressed)
                                seekBy(if (isLeftZone) -10_000L else 10_000L)
                            } else {
                                controlsVisible = !controlsVisible
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is MvPlayerUiState.Loading -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is MvPlayerUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = Color.White, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        MimikyuButton(
                            onClick = { viewModel.loadMvUrl(mvId, quality) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Retry", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
                is MvPlayerUiState.Blocked -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Streaming only allowed on Wi-Fi, currently on mobile network", color = Color.White, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MimikyuTextButton(onClick = onBack) { Text("Back", color = Color.White) }
                            Spacer(Modifier.width(16.dp))
                            MimikyuButton(
                                onClick = { viewModel.loadMvUrl(mvId, quality, forcePlayOnMobile = true) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Play Anyway", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
                is MvPlayerUiState.Success -> {
                    Box(
                        modifier = Modifier.fillMaxSize().aspectRatio(videoAspectRatio),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                SurfaceView(ctx).apply {
                                    holder.addCallback(object : SurfaceHolder.Callback {
                                        override fun surfaceCreated(holder: SurfaceHolder) {
                                            exoPlayer.setVideoSurfaceHolder(holder)
                                        }
                                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                                            exoPlayer.clearVideoSurfaceHolder(holder)
                                        }
                                    })
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (isBuffering) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }

            if (isEnded) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MimikyuIconButton(
                            onClick = { exoPlayer.seekTo(0); exoPlayer.play() },
                            modifier = Modifier.size(64.dp),
                            containerColor = Color.White.copy(alpha = 0.2f)
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = "Replay", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Playback ended", color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            playbackError?.let { message ->
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(message, color = Color.White, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        MimikyuButton(
                            onClick = {
                                val url = (uiState as? MvPlayerUiState.Success)?.videoUrl
                                if (url != null) {
                                    playbackError = null
                                    exoPlayer.setMediaItem(MediaItem.fromUri(url))
                                    exoPlayer.prepare()
                                    exoPlayer.playWhenReady = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Retry", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }

            seekFeedback?.let { text ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(text, color = Color.White, fontSize = 15.sp)
                }
            }

            gestureFeedback?.let { feedback ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(feedback.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { feedback.fraction },
                        modifier = Modifier.width(80.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
                            .then(if (isFullscreen) Modifier.statusBarsPadding() else Modifier)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MimikyuIconButton(onClick = { if (isFullscreen) exitFullscreen() else onBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        if (isFullscreen) {
                            Text(
                                text = mvDetail?.name ?: mvName,
                                color = Color.White,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        Box {
                            val qualityInteraction = remember { MutableInteractionSource() }
                            AssistChip(
                                onClick = { showQualityMenu = true },
                                interactionSource = qualityInteraction,
                                label = { Text("${quality}P", fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color.Black.copy(alpha = 0.35f),
                                    labelColor = Color.White,
                                    trailingIconContentColor = Color.White
                                ),
                                border = null,
                                modifier = Modifier
                                    .pressScale(MimikyuPress.Pill, qualityInteraction)
                                    .height(32.dp)
                            )
                            DropdownMenu(expanded = showQualityMenu, onDismissRequest = { showQualityMenu = false }) {
                                QUALITY_OPTIONS.forEach { q ->
                                    val itemInteraction = remember { MutableInteractionSource() }
                                    DropdownMenuItem(
                                        text = { Text("${q}P") },
                                        trailingIcon = if (q == quality) {
                                            { Icon(Icons.Default.Check, contentDescription = null) }
                                        } else null,
                                        interactionSource = itemInteraction,
                                        modifier = Modifier.pressHighlight(MimikyuPress.Row, itemInteraction),
                                        onClick = {
                                            showQualityMenu = false
                                            quality = q
                                            viewModel.loadMvUrl(mvId, q)
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                    }

                    if (!isBuffering && !isEnded && playbackError == null) {
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(36.dp)
                        ) {
                            MimikyuIconButton(onClick = { seekBy(-10_000L) }, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Rewind 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            MimikyuIconButton(
                                onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color.White, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            MimikyuIconButton(onClick = { seekBy(10_000L) }, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "Forward 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                            .then(if (isFullscreen) Modifier.navigationBarsPadding() else Modifier)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatMvTime(currentPosition)} / ${formatMvTime(duration)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            LinearProgressIndicator(
                                progress = { if (duration > 0) (bufferedPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f },
                                modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart),
                                color = Color.White.copy(alpha = 0.5f),
                                trackColor = Color.White.copy(alpha = 0.15f)
                            )
                            Slider(
                                value = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                                onValueChange = { fraction ->
                                    if (duration > 0) {
                                        val target = (fraction * duration).toLong()
                                        exoPlayer.seekTo(target)
                                        currentPosition = target
                                    }
                                },
                                thumb = {
                                    Box(
                                        modifier = Modifier.size(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(Modifier.size(10.dp).background(Color.White, CircleShape))
                                    }
                                },
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.Transparent
                                )
                            )
                        }
                        MimikyuIconButton(
                            onClick = { if (isFullscreen) exitFullscreen() else enterFullscreen() },
                            modifier = Modifier.size(32.dp).padding(start = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ：Songs/Follow/// MV  + Expand，
    // and ，（Column  vs Row ）
    @Composable
    fun InfoAndCommentsPanel(modifier: Modifier) {
        Box(
            modifier = modifier.background(BackgroundDark)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = LocalBottomOverlayInset.current + 16.dp)
            ) {
                item(key = "info") {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = mvDetail?.name ?: mvName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = buildString {
                                append(formatFansCount(mvDetail?.playCount ?: 0))
                                append(" views")
                                if (!mvDetail?.publishTime.isNullOrBlank()) {
                                    append(" · ")
                                    append(mvDetail?.publishTime)
                                }
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                item(key = "channel_actions_card") {
                    val detail = mvDetail
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(14.dp)
                    ) {
                        if (detail != null && detail.artistId > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data("${artistAvatar}?param=100y100")
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = detail.artistName,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable { onArtistClick(detail.artistId) }
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onArtistClick(detail.artistId) }
                                ) {
                                    Text(
                                        text = detail.artistName,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (fansCount > 0) {
                                        Text(
                                            text = "${formatFansCount(fansCount)} listeners",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                MimikyuButton(
                                    onClick = { viewModel.toggleArtistFollow() },
                                    colors = if (isArtistFollowed) {
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    },
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(if (isArtistFollowed) "Following" else "Follow", fontSize = 13.sp)
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MvActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.ThumbUp,
                                label = if ((detail?.likedCount ?: 0) > 0) formatFansCount(detail!!.likedCount) else "Like",
                                active = detail?.isLiked == true,
                                onClick = { viewModel.toggleLike(mvId) }
                            )
                            MvActionButton(
                                modifier = Modifier.weight(1f),
                                icon = if (detail?.isSubscribed == true) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                label = "Favorite",
                                active = detail?.isSubscribed == true,
                                onClick = { viewModel.toggleSubscribe(mvId) }
                            )
                            MvActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Share,
                                label = "Share",
                                active = false,
                                onClick = {
                                    val shareText = "《${detail?.name ?: mvName}》- ${detail?.artistName.orEmpty()} https://music.163.com/mv?id=$mvId"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share MV"))
                                }
                            )
                        }
                    }
                }

                val briefDesc = mvDetail?.briefDesc
                if (!briefDesc.isNullOrBlank()) {
                    item(key = "description") {
                        var expanded by remember { mutableStateOf(false) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { expanded = !expanded }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = briefDesc,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                maxLines = if (expanded) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (expanded) "Collapse" else "More",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }



                if (relatedMvs.isNotEmpty()) {
                    item(key = "related_header") {
                        Text(
                            text = "More MVs from this artist",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(relatedMvs, key = { it.id }) { mv ->
                        RelatedMvRow(mv = mv, onClick = { onMvClick(mv.id, mv.name) })
                    }
                }
            }


        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Crossfade(targetState = isFullscreen, label = "mv_fullscreen_toggle") { fullscreen ->
        if (fullscreen) {
            VideoArea(Modifier.fillMaxSize())
        } else if (useLandscapeSplitLayout) {
            // Expanded ： and
            Row(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                VideoArea(Modifier.weight(1.4f).fillMaxHeight())
                InfoAndCommentsPanel(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                VideoArea(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                InfoAndCommentsPanel(Modifier.weight(1f).fillMaxWidth())
            }
        }
        }


    }
}

@Composable
private fun MvActionButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .pressable(MimikyuPress.Pill, onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// "More MVs from this artist"： + /Play，"Play Next"
@Composable
private fun RelatedMvRow(
    mv: ArtistMv,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(RadiusCompact))
        ) {
            SubcomposeAsyncImage(
                model = "${mv.cover}?param=300y170",
                contentDescription = mv.name,
                contentScale = ContentScale.Crop,
                loading = { CoverPlaceholder() },
                error = { CoverPlaceholder() },
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mv.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${formatFansCount(mv.playCount)} views",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

private fun formatMvTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
