package com.lin0721.linmusic.core.player.domain

// / of
data class WordInfo(
    val text: String,
    val startOffsetMs: Long,  // of
    val durationMs: Long      // / of
) {
    val endOffsetMs: Long get() = startOffsetMs + durationMs
}


data class LyricLine(
    val timeMs: Long,
    val durationMs: Long = 0L, // of
    val text: String,
    val translation: String? = null,
    val roma: String? = null,
    val words: List<WordInfo> = emptyList(), // LRC；/
    val isGap: Boolean = false, // /，
    val isSynced: Boolean = true, // （ false ， and ）
    val isOppositeSide: Boolean = false // /Backing Vocals/（ The Weeknd & JENNIE，）
) {
    val endTimeMs: Long
        get() = if (durationMs > 0L) timeMs + durationMs else timeMs
}

// LazyColumn  of  key
fun lyricLineKey(index: Int, line: LyricLine): String = "${line.timeMs}_${line.isGap}_$index"
