package com.lin0721.linmusic.feature.recent.domain

import java.util.Calendar

// Calendar.DAY_OF_WEEK Sun(1)Sat(7)， and
private val WEEKDAY_NAMES = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

// of ，——
fun formatClockTime(timeMs: Long): String {
    if (timeMs <= 0) return ""
    val cal = Calendar.getInstance().apply { timeInMillis = timeMs }
    return String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
}

// of ，+，
fun formatDayHeaderLabel(timeMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    if (timeMs <= 0) return ""
    val target = Calendar.getInstance().apply { timeInMillis = timeMs }
    val now = Calendar.getInstance().apply { timeInMillis = nowMs }
    val month = target.get(Calendar.MONTH) + 1
    val day = target.get(Calendar.DAY_OF_MONTH)
    val weekday = WEEKDAY_NAMES[target.get(Calendar.DAY_OF_WEEK) - 1]
    val datePart = if (target.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
        "$month-$day"
    } else {
        "${target.get(Calendar.YEAR)}-$month-$day"
    }
    return "$datePart $weekday"
}
