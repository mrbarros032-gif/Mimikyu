package com.lin0721.linmusic.feature.recent.domain

// of ，label 「Today」「Yesterday」「08-24」
data class PlayDayGroup<T>(
    val label: String,
    val items: List<T>
)

// playTime Back， of ，
// Map 「Today」
fun <T> List<T>.groupByPlayDay(
    nowMs: Long = System.currentTimeMillis(),
    playTimeOf: (T) -> Long
): List<PlayDayGroup<T>> {
    val groups = mutableListOf<Pair<String, MutableList<T>>>()
    for (item in this) {
        val label = formatDayHeaderLabel(playTimeOf(item), nowMs)
        val current = groups.lastOrNull()?.takeIf { it.first == label }
        if (current != null) {
            current.second.add(item)
        } else {
            groups.add(label to mutableListOf(item))
        }
    }
    return groups.map { (label, items) -> PlayDayGroup(label, items) }
}
