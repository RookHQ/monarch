package dev.monarchui.internal

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import java.util.Date
import java.util.Locale

/**
 * Dates as Monarch passes them: epoch milliseconds at midnight UTC, the way
 * the Material 3 `DatePickerState` gives them (minSdk 24, no desugaring). A
 * date written with the device zone can show the day before in the Americas;
 * these helpers always write it in UTC.
 */
internal object MonarchUtcDates {
    /** One day in milliseconds. */
    const val DAY_MILLIS: Long = 86_400_000L

    private const val UTC = "UTC"
    private const val MONTH_DAY_SKELETON = "MMMd"

    /** The UTC zone, for formatters of dates. */
    val utc: TimeZone get() = TimeZone.getTimeZone(UTC)

    /** The date of [millis] in [locale], in UTC: "Mar 4, 2026" in English with the medium [style]. */
    fun dateText(millis: Long, locale: Locale, style: Int = DateFormat.MEDIUM): String =
        DateFormat.getDateInstance(style, locale).apply { timeZone = utc }.format(Date(millis))

    /** A writer of the abbreviated month and day, "Mar 4", in [locale] and UTC. Create it once per locale. */
    fun monthDay(locale: Locale): (Long) -> String {
        val format = DateFormat.getInstanceForSkeleton(MONTH_DAY_SKELETON, locale).apply { timeZone = utc }
        return { millis -> format.format(Date(millis)) }
    }

    /** Midnight UTC of the day that contains [millis]. */
    fun startOfDay(millis: Long): Long = Math.floorDiv(millis, DAY_MILLIS) * DAY_MILLIS

    /** The whole days from the day of [from] to the day of [to]; negative when [to] is earlier. */
    fun daysBetween(from: Long, to: Long): Long = (startOfDay(to) - startOfDay(from)) / DAY_MILLIS
}
