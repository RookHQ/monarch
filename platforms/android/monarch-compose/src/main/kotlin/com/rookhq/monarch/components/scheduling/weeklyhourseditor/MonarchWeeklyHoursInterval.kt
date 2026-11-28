package com.rookhq.monarch.components.scheduling.weeklyhourseditor

import androidx.compose.runtime.Immutable

/**
 * One available interval of a weekly schedule, the value of a `MonarchWeeklyHoursEditor`. A list of these is
 * the whole value, a plain shape that goes to a server unchanged, as on the web.
 *
 * @property day the ISO weekday: 1 is Monday and 7 is Sunday.
 * @property start the start, in minutes from midnight.
 * @property end the end, in minutes from midnight, exclusive and at most 1440 (the end of the day).
 */
@Immutable
public data class MonarchWeeklyHoursInterval(
    public val day: Int,
    public val start: Int,
    public val end: Int,
) {
    public companion object {
        /**
         * Checks every day of [value]: times fall within the day ([MonarchWeeklyHoursError.Range]), each interval
         * ends after it starts ([MonarchWeeklyHoursError.Order]) and no two intervals overlap
         * ([MonarchWeeklyHoursError.Overlap]). Returns the first problem per ISO weekday; an empty map means the
         * value is valid. The editor shows these under each day; the app decides whether to block saving.
         */
        public fun validate(value: List<MonarchWeeklyHoursInterval>): Map<Int, MonarchWeeklyHoursError> =
            buildMap {
                for (day in 1..DAYS) {
                    dayError(value.filter { it.day == day })?.let { put(day, it) }
                }
            }

        private fun dayError(ranges: List<MonarchWeeklyHoursInterval>): MonarchWeeklyHoursError? = when {
            ranges.any { it.start < 0 || it.end > MINUTES_PER_DAY } -> MonarchWeeklyHoursError.Range
            ranges.any { it.end <= it.start } -> MonarchWeeklyHoursError.Order
            ranges.sortedBy { it.start }.zipWithNext().any { (a, b) -> b.start < a.end } ->
                MonarchWeeklyHoursError.Overlap
            else -> null
        }

        private const val DAYS = 7
        private const val MINUTES_PER_DAY = 1440
    }
}
