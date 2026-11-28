package dev.monarchui.internal

import android.icu.text.DisplayContext
import android.icu.text.RelativeDateTimeFormatter
import android.icu.text.RelativeDateTimeFormatter.AbsoluteUnit
import android.icu.text.RelativeDateTimeFormatter.Direction
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit
import android.icu.util.ULocale
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

/**
 * One relative-time rule for every component, on every platform: the rule of
 * the web `formatRelativeTime` (`packages/react/src/components/data-display/
 * relative-time/relative-time.tsx`), written through ICU
 * (`RelativeDateTimeFormatter`, API 24) in the user's locale.
 *
 * The rule:
 * 1. Take the signed distance in seconds, `(time - now) / 1000`.
 * 2. Walk the units in order. For each, round the distance in that unit half
 *    up (JavaScript `Math.round`), and keep the first unit whose rounded
 *    value stays below its rollover:
 *    second (< 60), minute (60 s, < 60), hour (3 600 s, < 24),
 *    day (86 400 s, < 7), week (604 800 s, < 4.345),
 *    month (2 629 800 s, < 12), year (31 557 600 s, no limit).
 *    So 59.6 minutes is "1 hour ago", 6.6 days is "1 week ago".
 * 3. Write it with numeric `auto` by default: 0 seconds is "now", ±1 day
 *    "yesterday" / "tomorrow", ±1 week, month or year "last week" /
 *    "next month", and every other value a number ("3 hours ago",
 *    "in 2 days"). [format]'s `numeric = true` always writes a number.
 * 4. Never switch to a date by itself. A component that shows a date beyond
 *    some age passes its own threshold to [showsAbsolute] (the web
 *    `absoluteAfter`, strictly greater than).
 *
 * [refreshMillis] is the web `live` interval: 5 s in the first minute, then
 * 30 s, 5 min within a day, and an hour after that.
 */
internal object MonarchRelativeTime {
    private const val MILLIS_PER_SECOND = 1_000.0
    private const val MINUTE_MILLIS = 60_000L
    private const val HOUR_MILLIS = 3_600_000L
    private const val DAY_MILLIS = 86_400_000L
    private const val REFRESH_FRESH = 5_000L
    private const val REFRESH_MINUTES = 30_000L
    private const val REFRESH_HOURS = 300_000L
    private const val REFRESH_DAYS = 3_600_000L
    private const val HALF = 0.5
    private const val TWO_AWAY = 2L

    /** A unit of the rule: its length in seconds and the rounded value at which the next unit takes over. */
    @Suppress("MagicNumber") // The web's unit table, copied as it is.
    enum class Unit(
        internal val seconds: Double,
        internal val rollover: Double,
        internal val relative: RelativeUnit,
        internal val absolute: AbsoluteUnit?,
    ) {
        Second(1.0, 60.0, RelativeUnit.SECONDS, null),
        Minute(60.0, 60.0, RelativeUnit.MINUTES, null),
        Hour(3_600.0, 24.0, RelativeUnit.HOURS, null),
        Day(86_400.0, 7.0, RelativeUnit.DAYS, AbsoluteUnit.DAY),
        Week(604_800.0, 4.345, RelativeUnit.WEEKS, AbsoluteUnit.WEEK),
        Month(2_629_800.0, 12.0, RelativeUnit.MONTHS, AbsoluteUnit.MONTH),
        Year(31_557_600.0, Double.POSITIVE_INFINITY, RelativeUnit.YEARS, AbsoluteUnit.YEAR),
    }

    /** A signed value in a [unit]: negative in the past, positive in the future. */
    data class Parts(val value: Long, val unit: Unit)

    /** The length of the unit words. */
    enum class Style {
        /** "3 minutes ago". */
        Long,

        /** "3 min. ago", for tight rows. */
        Short,

        /** "3m ago" where the locale has a narrow form. */
        Narrow,
    }

    /** How the first letter is written. */
    enum class Case {
        /** As the locale writes the phrase on its own, as the web does: "now", "yesterday" in English. */
        Standalone,

        /** At the start of a sentence or a line: "Now", "Yesterday". */
        Sentence,

        /** Inside a sentence ("edited now"): lower case where the language allows. */
        MidSentence,
    }

    /** The unit and signed value for [time] relative to [now] (epoch milliseconds), by the rule above. */
    fun parts(time: Long, now: Long): Parts {
        val seconds = (time - now) / MILLIS_PER_SECOND
        for (unit in Unit.entries) {
            val value = jsRound(seconds / unit.seconds)
            if (abs(value.toDouble()) < unit.rollover) return Parts(value, unit)
        }
        return Parts(jsRound(seconds / Unit.Year.seconds), Unit.Year)
    }

    /**
     * [time] relative to [now], both epoch milliseconds, in [locale].
     *
     * @param future whether times after [now] are written as such ("in 5 minutes"), as on the web. When
     * `false`, a time in the future reads "now", which suits timestamps that a clock skew can put a little
     * ahead.
     * @param style the length of the unit words.
     * @param case how the first letter is written.
     * @param numeric always a number ("1 day ago"), the web `numeric: 'always'`. By default the words the
     * locale has are used ("yesterday", "now").
     */
    fun format(
        time: Long,
        now: Long,
        locale: Locale,
        future: Boolean = true,
        style: Style = Style.Long,
        case: Case = Case.Standalone,
        numeric: Boolean = false,
    ): String {
        val (value, unit) = parts(if (future) time else minOf(time, now), now)
        val formatter = formatter(locale, style, case)
        if (!numeric) words(formatter, value, unit)?.let { return it }
        val direction = if (value < 0) Direction.LAST else Direction.NEXT
        return formatter.format(abs(value).toDouble(), direction, unit.relative)
    }

    /**
     * Whether a component should show the absolute date instead: [time] is more than [absoluteAfter]
     * milliseconds from [now]. `null` never switches, which is the default of the rule.
     */
    fun showsAbsolute(time: Long, now: Long, absoluteAfter: Long?): Boolean =
        absoluteAfter != null && abs(now - time) > absoluteAfter

    /** How often a relative time of [time] should be redrawn at [now]: the web `live` interval. */
    fun refreshMillis(time: Long, now: Long): Long {
        val age = abs(now - time)
        return when {
            age < MINUTE_MILLIS -> REFRESH_FRESH
            age < HOUR_MILLIS -> REFRESH_MINUTES
            age < DAY_MILLIS -> REFRESH_HOURS
            else -> REFRESH_DAYS
        }
    }

    /** The interval for a screen that shows several [times]: the one the freshest of them needs. */
    fun refreshMillis(times: Iterable<Long?>, now: Long): Long =
        times.filterNotNull().minOfOrNull { refreshMillis(it, now) } ?: REFRESH_DAYS

    /** The locale's words for the values that have them (numeric `auto`), or `null` to write a number. */
    private fun words(formatter: RelativeDateTimeFormatter, value: Long, unit: Unit): String? {
        if (value == 0L) return formatter.format(Direction.PLAIN, AbsoluteUnit.NOW)
        val absolute = unit.absolute
        val direction = wordDirection(value, unit)
        // ICU has no "day before yesterday" in many locales (English among them): it then returns null.
        return if (absolute == null || direction == null) null else formatter.format(direction, absolute)
            ?.takeIf { it.isNotEmpty() }
    }

    /** The ICU direction with a word for [value] in [unit]: one away, or two days away. */
    private fun wordDirection(value: Long, unit: Unit): Direction? = when (value) {
        -1L -> Direction.LAST
        1L -> Direction.NEXT
        -TWO_AWAY -> Direction.LAST_2.takeIf { unit == Unit.Day }
        TWO_AWAY -> Direction.NEXT_2.takeIf { unit == Unit.Day }
        else -> null
    }

    /** JavaScript `Math.round`: halves go up, towards positive infinity (-2.5 is -2). */
    private fun jsRound(value: Double): Long = floor(value + HALF).toLong()

    private fun formatter(locale: Locale, style: Style, case: Case): RelativeDateTimeFormatter =
        RelativeDateTimeFormatter.getInstance(
            ULocale.forLocale(locale),
            null,
            when (style) {
                Style.Long -> RelativeDateTimeFormatter.Style.LONG
                Style.Short -> RelativeDateTimeFormatter.Style.SHORT
                Style.Narrow -> RelativeDateTimeFormatter.Style.NARROW
            },
            when (case) {
                Case.Standalone -> DisplayContext.CAPITALIZATION_NONE
                Case.Sentence -> DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE
                Case.MidSentence -> DisplayContext.CAPITALIZATION_FOR_MIDDLE_OF_SENTENCE
            },
        )
}
