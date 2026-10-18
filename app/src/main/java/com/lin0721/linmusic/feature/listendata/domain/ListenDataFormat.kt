package com.lin0721.linmusic.feature.listendata.domain

import java.util.Calendar
import java.util.Locale

// Back of  of ， of
private val PERIOD_ORDER = listOf(
    "early_morning", "morning", "noon", "afternoon", "night", "deep_night"
)

private val PERIOD_LABELS = mapOf(
    "early_morning" to "Early Morning",
    "morning" to "Morning",
    "noon" to "Noon",
    "afternoon" to "Afternoon",
    "night" to "Evening",
    "deep_night" to "Late Night"
)

private val WEEKDAY_LABELS = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")


private val PERCENT_REGEX = Regex("""(\d{1,3})\s*%""")

fun sortedPeriodKeys(): List<String> = PERIOD_ORDER

fun periodLabel(period: String): String = PERIOD_LABELS[period] ?: period

fun periodSortIndex(period: String): Int =
    PERIOD_ORDER.indexOf(period).let { if (it < 0) PERIOD_ORDER.size else it }

// → ，
fun toHours(seconds: Long): Long = if (seconds <= 0) 0 else seconds / 3600

// →  of ，
fun toHoursText(seconds: Long): String =
    if (seconds <= 0) "0" else String.format(Locale.US, "%.1f", seconds / 3600.0)

fun cleanBeyondPercentText(raw: String): String {
    val percent = PERCENT_REGEX.find(raw)?.groupValues?.getOrNull(1) ?: return ""
    return "More than $percent% of users"
}

// 「2000」→「2000s」，
fun ageLabel(age: String): String = if (age.isBlank()) "" else "${age}s"

// 「2026-08-23」→ ；
fun weekdayLabel(period: String): String {
    val parts = period.split("-")
    if (parts.size != 3) return period
    val year = parts[0].toIntOrNull() ?: return period
    val month = parts[1].toIntOrNull() ?: return period
    val day = parts[2].toIntOrNull() ?: return period
    return runCatching {
        val cal = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day)
        }
        WEEKDAY_LABELS[cal.get(Calendar.DAY_OF_WEEK) - 1]
    }.getOrElse { monthDayLabel(period) }
}

// 「2026-08-23」→「23」。， dp， of
fun monthDayLabel(period: String): String {
    val day = period.split("-").getOrNull(2)?.trimStart('0').orEmpty()
    return day.ifBlank { period }
}

// of 。，
fun fullWeekdayLabel(period: String): String {
    val label = weekdayLabel(period)
    return if (label.length == 1) "$label" else label
}

fun fullMonthDayLabel(period: String): String {
    val parts = period.split("-")
    val month = parts.getOrNull(1)?.trimStart('0')
    val day = parts.getOrNull(2)?.trimStart('0')
    return if (month.isNullOrBlank() || day.isNullOrBlank()) period else "$month-$day"
}
