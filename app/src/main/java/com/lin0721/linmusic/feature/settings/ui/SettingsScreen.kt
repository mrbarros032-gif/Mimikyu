package com.lin0721.linmusic.feature.settings.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.components.AdaptiveContentWidth
import com.lin0721.linmusic.core.ui.components.MimikyuIconButton
import com.lin0721.linmusic.core.ui.components.MimikyuButton
import com.lin0721.linmusic.core.ui.components.MimikyuSwitch
import com.lin0721.linmusic.LocalBottomOverlayInset
import com.lin0721.linmusic.LocalGlobalOverlayOpen
import com.lin0721.linmusic.core.ui.components.ToastManager
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

// 。sectionTitles  and  SettingsGroupCard  of ，；
enum class SettingsSubMenu(val title: String, val sectionTitles: List<String>, val icon: ImageVector) {
    PLAYBACK_DOWNLOAD("Playback & Downloads", listOf("Playback Settings", "Downloads & Cache"), Icons.Outlined.PlayCircleOutline),
    AUDIO_QUALITY("Audio Quality", listOf("Default Quality"), Icons.Outlined.HighQuality),
    PRIVACY("Privacy Settings", emptyList(), Icons.Outlined.PrivacyTip),
    STORAGE("Storage", listOf("Storage Management"), Icons.Outlined.Storage),
    NETWORK("Network Settings", listOf("Network Connection"), Icons.Outlined.Wifi),
    EXTENSIONS("Extensions", listOf("Floating & Desktop", "Device & Integration", "Navigation Bar"), Icons.Outlined.Extension),
    ABOUT("About", listOf("Version Updates", "App Info & License"), Icons.Outlined.Info);

    val subtitle: String get() = sectionTitles.take(2).joinToString("、")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    var activeSubMenu by remember { mutableStateOf<SettingsSubMenu?>(null) }

    // ， of Back
    BackHandler(enabled = activeSubMenu != null && !LocalGlobalOverlayOpen.current) {
        activeSubMenu = null
    }
    
    // Toast Messages
    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collectLatest { msg ->
            ToastManager.showToast(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = activeSubMenu?.title ?: "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    MimikyuIconButton(
                        onClick = {
                            if (activeSubMenu != null) {
                                activeSubMenu = null
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        AdaptiveContentWidth(
            modifier = Modifier.padding(innerPadding)
        ) {
            // AnimatedContent  of
            AnimatedContent(
                targetState = activeSubMenu,
                transitionSpec = {
                    if (targetState != null) {
                        // ：
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        // Back：
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "SettingsMenuTransition"
            ) { subMenu ->
                if (subMenu == null) {

                    MainSettingsMenu(
                        onNavigate = { activeSubMenu = it },
                        onBack = onBack,
                        viewModel = viewModel
                    )
                } else {

                    SubMenuContent(
                        subMenu = subMenu,
                        viewModel = viewModel,
                        context = context
                    )
                }
            }
        }
    }
}


@Composable
private fun MainSettingsMenu(
    onNavigate: (SettingsSubMenu) -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MimikyuSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MimikyuSpacing.md),
        contentPadding = PaddingValues(bottom = LocalBottomOverlayInset.current + 16.dp, top = 8.dp)
    ) {

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "General Settings",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = MimikyuSpacing.xs, bottom = MimikyuSpacing.sm)
                )
                SettingsSubMenu.values().forEach { item ->
                    SettingsMainMenuRow(
                        icon = item.icon,
                        title = item.title,
                        subtitle = item.subtitle,
                        onClick = { onNavigate(item) }
                    )
                }
            }
        }
    }
}


@Composable
private fun SubMenuContent(
    subMenu: SettingsSubMenu,
    viewModel: SettingsViewModel,
    context: Context
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MimikyuSpacing.md)
    ) {
        when (subMenu) {
            SettingsSubMenu.PLAYBACK_DOWNLOAD -> PlaybackDownloadSettingsView(viewModel)
            SettingsSubMenu.AUDIO_QUALITY -> AudioQualitySettingsView(viewModel)
            SettingsSubMenu.PRIVACY -> PrivacySettingsView(viewModel)
            SettingsSubMenu.STORAGE -> StorageSettingsView(viewModel, context)
            SettingsSubMenu.NETWORK -> NetworkSettingsView(viewModel)
            SettingsSubMenu.EXTENSIONS -> ExtensionsSettingsView(viewModel)
            SettingsSubMenu.ABOUT -> AboutSettingsView(viewModel)
        }
    }
}

// ───  ( private，) ───

@Composable
fun SettingsGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = MimikyuSpacing.xs, bottom = MimikyuSpacing.sm)
        )
        Column(content = content)
    }
}

@Composable
private fun SettingsMainMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(MimikyuPress.Row, onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(MimikyuSpacing.md))
        Column(modifier = Modifier.weight(1f).padding(end = MimikyuSpacing.sm)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(MimikyuPress.Row, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = MimikyuSpacing.sm)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = MimikyuSpacing.md)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        MimikyuSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

fun getBindingIcon(type: Int): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        1 -> Icons.Default.PhoneAndroid
        10 -> Icons.Default.ChatBubbleOutline
        20 -> Icons.Default.AccountCircle
        else -> Icons.Default.Link
    }
}
