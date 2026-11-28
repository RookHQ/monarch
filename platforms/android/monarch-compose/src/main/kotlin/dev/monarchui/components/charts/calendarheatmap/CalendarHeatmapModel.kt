package dev.monarchui.components.charts.calendarheatmap

import dev.monarchui.components.charts.heatmap.HeatmapScale

/**
 * Calendar arithmetic on day numbers: whole days since 1970-01-01 in UTC. Pure, so it is the same on every
 * device whatever its time zone, and needs no `java.time` (minSdk 24).
 */
internal object CalendarHeatmapDates {
    /** The milliseconds of one day. */
    const val DAY_MILLIS: Long = 86_400_000L

    /** The days of a week. */
    const val WEEK: Int = 7

    /** 1970-01-01 was a Thursday: the weekday of day 0, counting Sunday as 0. */
    private const val EPOCH_WEEKDAY = 4

    private const val DAYS_PER_ERA = 146_097L
    private const val ERA_SHIFT = 719_468L
    private const val DAYS_PER_YEAR = 365L
    private const val LEAP_EVERY = 4L
    private const val CENTURY = 100L
    private const val ERA_DAYS_LAST = 146_096L
    private const val ERA_FOUR_YEARS = 1_460L
    private const val ERA_CENTURY = 36_524L
    private const val MONTH_SPAN = 153L
    private const val MONTH_OFFSET = 2L
    private const val MONTH_ROUND = 5L

    /** The day number of [millis]. */
    fun dayNumber(millis: Long): Long = Math.floorDiv(millis, DAY_MILLIS)

    /** The epoch milliseconds of the start of [day]. */
    fun millis(day: Long): Long = day * DAY_MILLIS

    /** The weekday of [day], 0 for Sunday to 6 for Saturday. */
    fun weekday(day: Long): Int = Math.floorMod(day + EPOCH_WEEKDAY, WEEK.toLong()).toInt()

    /** The day of the month of [day], 1 to 31, by the proleptic Gregorian calendar. */
    fun dayOfMonth(day: Long): Int {
        val z = day + ERA_SHIFT
        val era = Math.floorDiv(z, DAYS_PER_ERA)
        val doe = z - era * DAYS_PER_ERA
        val yoe = (doe - doe / ERA_FOUR_YEARS + doe / ERA_CENTURY - doe / ERA_DAYS_LAST) / DAYS_PER_YEAR
        val doy = doe - (DAYS_PER_YEAR * yoe + yoe / LEAP_EVERY - yoe / CENTURY)
        val mp = (MONTH_ROUND * doy + MONTH_OFFSET) / MONTH_SPAN
        return (doy - (MONTH_SPAN * mp + MONTH_OFFSET) / MONTH_ROUND + 1).toInt()
    }
}

/**
 * The days of a calendar heatmap, built once from its entries and range.
 *
 * @param firstWeekday the first day of the week, 0 for Sunday to 6 for Saturday.
 */
internal class CalendarHeatmapModel(
    days: List<MonarchCalendarHeatmapDay>,
    from: Long?,
    to: Long?,
    firstWeekday: Int,
    thresholds: List<Double>?,
) {
    /** The entries by day number; a later entry for the same day wins. */
    private val byDay: Map<Long, MonarchCalendarHeatmapDay> =
        days.associateBy { CalendarHeatmapDates.dayNumber(it.date) }

    /** The days drawn, from the first to the last; `null` when the range is empty. */
    val range: LongRange? = run {
        val first = from?.let(CalendarHeatmapDates::dayNumber) ?: byDay.keys.minOrNull()
        val last = to?.let(CalendarHeatmapDates::dayNumber) ?: byDay.keys.maxOrNull()
        if (first != null && last != null && last >= first) first..last else null
    }

    /** Whether the range has at least one day. */
    val valid: Boolean = range != null

    /** The number of days in the range. */
    val count: Int = range?.let { (it.last - it.first + 1).toInt() } ?: 0

    /** The empty places before the first day in its week. */
    val lead: Int = range?.let {
        Math.floorMod(CalendarHeatmapDates.weekday(it.first) - firstWeekday, CalendarHeatmapDates.WEEK)
    } ?: 0

    /** The day number of the first place of the grid, the first weekday of the first week. */
    val gridStart: Long = (range?.first ?: 0L) - lead

    /** The number of weeks drawn. */
    val weeks: Int = if (valid) (lead + count + CalendarHeatmapDates.WEEK - 1) / CalendarHeatmapDates.WEEK else 0

    /** The entries in the range, by day. */
    val entries: List<Pair<Long, MonarchCalendarHeatmapDay>> =
        byDay.entries.filter { inRange(it.key) }.sortedBy { it.key }.map { it.key to it.value }

    /** The four-step scale of the values in the range. */
    val scale: HeatmapScale = HeatmapScale.of(entries.map { it.second.value }, thresholds)

    /** The sum of the finite values in the range. */
    val total: Double = entries.sumOf { finite(it.second) ?: 0.0 }

    /** The day of the highest value; the first one on a tie. */
    val highest: Long? = entries
        .mapNotNull { (day, entry) -> finite(entry)?.let { day to it } }
        .maxByOrNull { it.second }
        ?.first

    /** Whether any entry has a note, which adds the Note column to the table. */
    val hasNotes: Boolean = entries.any { it.second.note != null }

    /** Whether there is anything to show: an entry in the range. */
    val hasData: Boolean = entries.isNotEmpty()

    /** Whether a day in the range has no value, which adds the No data step to the legend. */
    val hasGaps: Boolean = range?.any { finite(byDay[it]) == null } ?: false

    /** Whether [day] is in the range. */
    fun inRange(day: Long): Boolean = range?.contains(day) ?: false

    /** The entry of [day], or `null`. */
    fun entry(day: Long): MonarchCalendarHeatmapDay? = byDay[day]

    /** The entry of [day], or a day without a value: what `onSelect` receives. */
    fun dayAt(day: Long): MonarchCalendarHeatmapDay =
        byDay[day] ?: MonarchCalendarHeatmapDay(CalendarHeatmapDates.millis(day), null)

    /** The level of [day]: `null` for no data, 0 for zero or less, 1 to 4 up the scale. */
    fun level(day: Long): Int? = scale.level(finite(byDay[day]))

    /** The day at grid place [index], counted from [gridStart]. */
    fun dayOfPlace(index: Int): Long = gridStart + index

    companion object {
        /** The finite value of [day], or `null`. */
        fun finite(day: MonarchCalendarHeatmapDay?): Double? = day?.value?.takeIf { it.isFinite() }
    }
}
