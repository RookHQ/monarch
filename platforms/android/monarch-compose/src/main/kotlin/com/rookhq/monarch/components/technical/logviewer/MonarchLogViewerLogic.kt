package com.rookhq.monarch.components.technical.logviewer

import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** The pure logic of MonarchLogViewer: filtering, matching and formatting. */
internal object MonarchLogViewerLogic {
    /** The search text as it is matched: trimmed and in lower case. Empty when there is no search. */
    fun normalized(query: String): String = query.trim().lowercase(Locale.ROOT)

    /** Whether [line] matches the normalised [query]: its message or source contains it, ignoring case. */
    fun matches(line: MonarchLogLine, query: String): Boolean = query.isEmpty() ||
        line.message.contains(query, ignoreCase = true) ||
        line.source?.contains(query, ignoreCase = true) == true

    /** The indices in [lines] of the lines shown: their level is on and, in filter mode, they match. */
    fun visible(
        lines: List<MonarchLogLine>,
        levels: Set<MonarchLogLevel>,
        query: String,
        mode: MonarchLogViewerSearchMode,
    ): List<Int> {
        val normalized = normalized(query)
        return lines.indices.filter { index ->
            val line = lines[index]
            line.level in levels &&
                (mode == MonarchLogViewerSearchMode.Highlight || matches(line, normalized))
        }
    }

    /** How many lines each level has. */
    fun counts(lines: List<MonarchLogLine>): Map<MonarchLogLevel, Int> =
        MonarchLogLevel.entries.associateWith { level -> lines.count { it.level == level } }

    /** [levels] with [level] turned on or off. */
    fun toggled(levels: Set<MonarchLogLevel>, level: MonarchLogLevel): Set<MonarchLogLevel> =
        if (level in levels) levels - level else levels + level

    /** The ranges of [text] that match the normalised [query], ignoring case, without overlaps. */
    fun matchRanges(text: String, query: String): List<IntRange> {
        if (query.isEmpty()) return emptyList()
        val ranges = mutableListOf<IntRange>()
        var from = text.indexOf(query, ignoreCase = true)
        while (from >= 0) {
            ranges += from until from + query.length
            from = text.indexOf(query, startIndex = from + query.length, ignoreCase = true)
        }
        return ranges
    }

    /** The time of a line as 24-hour `HH:mm:ss.SSS` in [zone], with Western digits. */
    fun formatTime(millis: Long, zone: TimeZone): String {
        val calendar = Calendar.getInstance(zone, Locale.ROOT).apply { timeInMillis = millis }
        return String.format(
            Locale.ROOT,
            "%02d:%02d:%02d.%03d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND),
            calendar.get(Calendar.MILLISECOND),
        )
    }

    /** A 1-based line number, padded to the line number column. */
    fun lineNumber(index: Int): String = (index + 1).toString().padStart(MonarchLogViewerTokens.LINE_NUMBER_DIGITS)

    /** A count in the digits and grouping of [locale]. */
    fun count(value: Int, locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(value)

    /** How many lines arrived since following was paused. */
    fun newLines(total: Int, totalAtPause: Int): Int = (total - totalAtPause).coerceAtLeast(0)
}
