package com.rookhq.monarch.components.intelligence.aiconversation

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** ISO 8601 times as the web writes them: UTC with milliseconds, for example `2025-03-10T12:00:00.000Z`. */
internal object MonarchAiTimestamp {
    private const val WITH_MILLIS = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    private const val WITHOUT_MILLIS = "yyyy-MM-dd'T'HH:mm:ssXXX"

    private fun format(pattern: String): SimpleDateFormat =
        SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }

    /** The text of a time, with `Z` for UTC. */
    fun text(millis: Long): String = format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(java.util.Date(millis))

    /** The time of an ISO 8601 text, with or without milliseconds, or `null`. */
    fun millis(text: String): Long? = listOf(WITH_MILLIS, WITHOUT_MILLIS).firstNotNullOfOrNull { pattern ->
        try {
            format(pattern).parse(text)?.time
        } catch (_: ParseException) {
            null
        }
    }
}
