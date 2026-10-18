package com.lin0721.linmusic.core.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Universal date parser that handles string formats ("YYYY-MM-DD", "YYYY-MM", "YYYY", etc.),
 * 4-digit years (1982), epoch seconds, and epoch milliseconds, returning UTC epoch milliseconds.
 */
fun parseDateToEpochMillis(raw: Any?): Long {
    if (raw == null) return 0L
    return when (raw) {
        is Long -> parseLongDate(raw)
        is Int -> parseLongDate(raw.toLong())
        is String -> parseStringDate(raw)
        else -> 0L
    }
}

private fun parseLongDate(value: Long): Long {
    if (value <= 0L) return 0L
    return when {
        // 4-digit year: e.g. 1982 -> convert to UTC timestamp for Jan 1 of that year
        value in 1000L..9999L -> {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(value.toInt(), 0, 1, 0, 0, 0)
            }
            cal.timeInMillis
        }
        // Epoch seconds (e.g. 10 digits) -> convert to epoch millis
        value in 100_000_000L..99_999_999_999L -> value * 1000L
        // Epoch milliseconds (>= 100_000_000_000L)
        value >= 100_000_000_000L -> value
        else -> 0L
    }
}

private fun parseStringDate(str: String): Long {
    val clean = str.trim()
    if (clean.isBlank()) return 0L

    // Direct number check (epoch millis, seconds, or year)
    val num = clean.toLongOrNull()
    if (num != null) {
        return parseLongDate(num)
    }

    val patterns = listOf(
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "yyyy.MM.dd",
        "yyyy-MM",
        "yyyy/MM",
        "MM/dd/yyyy",
        "dd-MM-yyyy",
        "MMM dd, yyyy",
        "MMMM d, yyyy",
        "yyyy"
    )

    for (pattern in patterns) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }
            val date = sdf.parse(clean)
            if (date != null) return date.time
        } catch (_: Exception) {}
    }

    // Fallback: extract 4-digit year regex
    val yearMatch = Regex("""\b(19\d\d|20\d\d)\b""").find(clean)
    if (yearMatch != null) {
        val year = yearMatch.value.toLongOrNull()
        if (year != null) return parseLongDate(year)
    }

    return 0L
}

/**
 * Formats an album release date from epoch millis or year.
 * If only the year was given (e.g. 1982 or Jan 1st 00:00:00), displays "1982".
 * If an exact date was given, displays "YYYY-MM-DD".
 * Avoids any false "1970 01 01" bugs.
 */
fun formatAlbumReleaseDate(publishTime: Long): String {
    if (publishTime <= 0L) return ""
    return when {
        publishTime in 1000L..9999L -> publishTime.toString()
        else -> {
            val millis = if (publishTime in 100_000_000L..99_999_999_999L) publishTime * 1000L else publishTime
            val date = Date(millis)
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = date }
            if (cal.get(Calendar.MONTH) == 0 && cal.get(Calendar.DAY_OF_MONTH) == 1 &&
                cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) == 0 &&
                cal.get(Calendar.MILLISECOND) == 0
            ) {
                cal.get(Calendar.YEAR).toString()
            } else {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                sdf.format(date)
            }
        }
    }
}

/**
 * Flexible serializer for publishTime that seamlessly decodes String dates ("1982-11-30", "1982"),
 * year numbers (1982), epoch seconds, and epoch milliseconds into epoch milliseconds.
 */
object FlexibleDateEpochMillisSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleDateEpochMillis", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value)
    }

    override fun deserialize(decoder: Decoder): Long {
        return try {
            val jsonDecoder = decoder as? JsonDecoder
            if (jsonDecoder != null) {
                val element = jsonDecoder.decodeJsonElement()
                when (element) {
                    is JsonPrimitive -> {
                        element.longOrNull?.let { parseDateToEpochMillis(it) }
                            ?: parseDateToEpochMillis(element.content)
                    }
                    else -> 0L
                }
            } else {
                decoder.decodeLong()
            }
        } catch (_: Exception) {
            0L
        }
    }
}
