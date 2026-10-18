package com.lin0721.linmusic.core.common

import java.util.Locale

private const val KB = 1024.0
private const val MB = KB * 1024
private const val GB = MB * 1024

fun formatFileSize(bytes: Long): String = when {
    bytes <= 0 -> "0 KB"
    bytes < MB -> String.format(Locale.US, "%.0f KB", bytes / KB)
    bytes < GB -> String.format(Locale.US, "%.1f MB", bytes / MB)
    else -> String.format(Locale.US, "%.1f GB", bytes / GB)
}

fun formatWholeGigabytes(bytes: Long): Int = (bytes / GB).toInt()
