package com.rookhq.monarch.components.charts.calendarheatmap

import java.util.Locale

/** The resolved strings, number and date formats of a calendar heatmap. Templates use `%1$s`-style placeholders. */
@Suppress("LongParameterList")
internal class CalendarHeatmapWords(
    val locale: Locale,
    val format: (Double) -> String,
    val fullDate: (Long) -> String,
    val noData: String,
    val today: String,
    val inactive: String,
    val marker: String,
    val day: String,
    val dayCount: (Int) -> String,
    val summary: String,
    val highest: String,
)

/** The text of a calendar heatmap: values, what TalkBack reads for a day, the summary. Pure. */
internal object CalendarHeatmapText {
    private const val SEPARATOR = ", "
    private const val SENTENCE = ". "

    /** The value as written: the entry's label, the formatted value or No data. */
    fun valueText(entry: MonarchCalendarHeatmapDay?, words: CalendarHeatmapWords): String =
        entry?.valueLabel ?: CalendarHeatmapModel.finite(entry)?.let(words.format) ?: words.noData

    /** The value followed by the note, Non-working day, Marked and Today, as each applies. */
    fun details(model: CalendarHeatmapModel, day: Long, today: Long?, words: CalendarHeatmapWords): String {
        val entry = model.entry(day)
        val parts = listOfNotNull(
            valueText(entry, words),
            entry?.note,
            words.inactive.takeIf { entry?.inactive == true },
            words.marker.takeIf { entry?.marker == true },
            words.today.takeIf { day == today },
        )
        return parts.joinToString(SEPARATOR)
    }

    /** What TalkBack reads for a day and the inspection line shows: "Thu, Jan 1, 2026: 12, Holiday". */
    fun describe(model: CalendarHeatmapModel, day: Long, today: Long?, words: CalendarHeatmapWords): String =
        String.format(words.locale, words.day, words.fullDate(day), details(model, day, today, words))

    /**
     * The generated summary: the number of days, the first and last day, the total, and the highest day.
     * "181 days from Thu, Jan 1, 2026 to Tue, Jun 30, 2026. Total 812. Highest: Mon, Mar 2, 2026: 14".
     */
    fun summary(model: CalendarHeatmapModel, words: CalendarHeatmapWords): String {
        val range = model.range ?: return words.noData
        val first = String.format(
            words.locale,
            words.summary,
            words.dayCount(model.count),
            words.fullDate(range.first),
            words.fullDate(range.last),
            words.format(model.total),
        )
        val highest = model.highest?.let { day ->
            val described = String.format(
                words.locale,
                words.day,
                words.fullDate(day),
                valueText(model.entry(day), words),
            )
            String.format(words.locale, words.highest, described)
        }
        return listOfNotNull(first, highest).joinToString(SENTENCE)
    }
}
