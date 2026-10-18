package com.lin0721.linmusic.feature.settings.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.LocalBottomOverlayInset
import com.lin0721.linmusic.core.ui.components.MimikyuTextButton
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.ui.theme.DragHandleShape
import com.lin0721.linmusic.core.ui.theme.EntryHotGradient
import com.lin0721.linmusic.core.ui.theme.EntryRadarGradient
import com.lin0721.linmusic.core.ui.theme.EntryRoamingGradient
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.NeteaseRed
import com.lin0721.linmusic.core.ui.theme.TextGray
import com.lin0721.linmusic.core.common.formatFileSize

// 5  of ， of ，
private val CacheAudioColor = NeteaseRed
private val CacheImageColor = EntryHotGradient.first()
private val CacheLogColor = EntryRadarGradient.first()
private val CacheUpdateColor = EntryRoamingGradient.first()
private val CacheOtherColor = TextGray

// of
private data class PendingClear(val title: String, val message: String, val action: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettingsView(viewModel: SettingsViewModel, context: Context) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val currentMaxSize by viewModel.audioCacheMaxSize.collectAsStateWithLifecycle()
    val audioCacheSize by viewModel.audioCacheSize.collectAsStateWithLifecycle()
    val imageCacheSize by viewModel.imageCacheSize.collectAsStateWithLifecycle()
    val logCacheSize by viewModel.logCacheSize.collectAsStateWithLifecycle()
    val updatePackageSize by viewModel.updatePackageSize.collectAsStateWithLifecycle()
    val otherCacheSize by viewModel.otherCacheSize.collectAsStateWithLifecycle()
    val totalCacheSize by viewModel.totalCacheSize.collectAsStateWithLifecycle()
    var showSheet by remember { mutableStateOf(false) }
    var pendingClear by remember { mutableStateOf<PendingClear?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadStorageStats(context)
    }

    val currentSizeStr = when (currentMaxSize) {
        500 * 1024 * 1024L -> "500 MB"
        1024 * 1024 * 1024L -> "1 GB"
        2 * 1024 * 1024 * 1024L -> "2 GB"
        5 * 1024 * 1024 * 1024L -> "5 GB"
        Long.MAX_VALUE -> "Unlimited"
        else -> "${currentMaxSize / (1024 * 1024)} MB"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(MimikyuSpacing.md),
            contentPadding = PaddingValues(top = 8.dp, bottom = LocalBottomOverlayInset.current + 16.dp)
        ) {
            item {
                SettingsGroupCard(SettingsSubMenu.STORAGE.sectionTitles[0]) {
                    CacheUsageBar(
                        totalSize = totalCacheSize,
                        audioCacheSize = audioCacheSize,
                        imageCacheSize = imageCacheSize,
                        logCacheSize = logCacheSize,
                        updatePackageSize = updatePackageSize,
                        otherCacheSize = otherCacheSize
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    // All
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                pendingClear = PendingClear(
                                    title = "Clear App Cache",
                                    message = "Are you sure you want to clear all the following cache?"
                                ) { viewModel.clearApplicationCache(context) }
                            }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Clear App Cache", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                            Text("Clear all cached data below", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    CacheCategoryRow(
                        title = "Audio Cache",
                        subtitle = "Buffered song audio data in player",
                        sizeBytes = audioCacheSize,
                        onClear = {
                            pendingClear = PendingClear(
                                title = "Clear Audio Cache",
                                message = "Are you sure you want to clear audio cache?"
                            ) { viewModel.clearAudioCacheOnly(context) }
                        }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    CacheCategoryRow(
                        title = "Image Cache",
                        subtitle = "Memory and disk cache for covers and avatars",
                        sizeBytes = imageCacheSize,
                        onClear = {
                            pendingClear = PendingClear(
                                title = "Clear Image Cache",
                                message = "Are you sure you want to clear image cache?"
                            ) { viewModel.clearImageCacheOnly(context) }
                        }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    CacheCategoryRow(
                        title = "Log Files",
                        subtitle = "Diagnostic logs; will no longer be exportable once cleared",
                        sizeBytes = logCacheSize,
                        onClear = {
                            pendingClear = PendingClear(
                                title = "Clear Log Files",
                                message = "Are you sure you want to clear log files?"
                            ) { viewModel.clearLogCacheOnly() }
                        }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    CacheCategoryRow(
                        title = "Update Packages",
                        subtitle = "Old installation packages downloaded during app updates",
                        sizeBytes = updatePackageSize,
                        onClear = {
                            pendingClear = PendingClear(
                                title = "Clear Update Packages",
                                message = "Are you sure you want to clear old app update packages?"
                            ) { viewModel.clearUpdatePackages(context) }
                        }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    CacheCategoryRow(
                        title = "Other Temporary Files",
                        subtitle = "Other uncategorized temporary app data",
                        sizeBytes = otherCacheSize,
                        onClear = {
                            pendingClear = PendingClear(
                                title = "Clear Other Temp Files",
                                message = "Are you sure you want to clear other temporary files?"
                            ) { viewModel.clearOtherTempFiles(context) }
                        }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))


                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSheet = true }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Max Audio Cache Limit", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                            Text("Current limit: $currentSizeStr", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Text(
                            text = "Change",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }


        if (showSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.background,
                shape = BottomSheetShape,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = MimikyuSpacing.xs)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(DragHandleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = MimikyuSpacing.lg, end = MimikyuSpacing.lg, bottom = MimikyuSpacing.lg)
                ) {
                    Text(
                        text = "Max Audio Cache Limit",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = MimikyuSpacing.md)
                    )

                    val options = listOf(
                        500 * 1024 * 1024L to "500 MB",
                        1024 * 1024 * 1024L to "1 GB",
                        2 * 1024 * 1024 * 1024L to "2 GB",
                        5 * 1024 * 1024 * 1024L to "5 GB",
                        Long.MAX_VALUE to "Unlimited"
                    )
                    options.forEach { (size, label) ->
                        val isSelected = currentMaxSize == size
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateAudioCacheMaxSize(context, size)
                                    showSheet = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }


        pendingClear?.let { pending ->
            ClearConfirmDialog(
                title = pending.title,
                message = pending.message,
                onConfirm = {
                    pending.action()
                    pendingClear = null
                },
                onDismiss = { pendingClear = null }
            )
        }


        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ：，， 0
@Composable
private fun CacheUsageBar(
    totalSize: Long,
    audioCacheSize: Long,
    imageCacheSize: Long,
    logCacheSize: Long,
    updatePackageSize: Long,
    otherCacheSize: Long
) {
    val categories = listOf(
        Triple("Audio Cache", audioCacheSize, CacheAudioColor),
        Triple("Image Cache", imageCacheSize, CacheImageColor),
        Triple("Log Files", logCacheSize, CacheLogColor),
        Triple("Update Packages", updatePackageSize, CacheUpdateColor),
        Triple("Other Temporary Files", otherCacheSize, CacheOtherColor)
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = "Currently using ${formatFileSize(totalSize)}",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(MimikyuSpacing.sm))

        if (totalSize > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
            ) {
                categories.forEach { (_, size, color) ->
                    if (size > 0) {
                        Box(
                            modifier = Modifier
                                .weight(size.toFloat())
                                .fillMaxHeight()
                                .background(color)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }

        Spacer(modifier = Modifier.height(MimikyuSpacing.sm))

        categories.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MimikyuSpacing.md)
            ) {
                rowItems.forEach { (label, size, color) ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$label ${formatFileSize(size)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

// ：，Clear
@Composable
private fun CacheCategoryRow(
    title: String,
    subtitle: String,
    sizeBytes: Long,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = sizeBytes > 0) { onClear() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = MimikyuSpacing.md)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Text(
            text = formatFileSize(sizeBytes),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        if (sizeBytes > 0) {
            Spacer(modifier = Modifier.width(MimikyuSpacing.sm))
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Clear $title",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@Composable
private fun ClearConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
        confirmButton = {
            MimikyuTextButton(onClick = onConfirm) {
                Text("Clear", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            MimikyuTextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    )
}
