package com.rookhq.monarch.components.scheduling.teamcalendar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.scheduling.MonarchCalendarDates
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * The colours of a bar of a team calendar.
 *
 * @property container the fill; the surface colour for a pending entry.
 * @property stripe the solid edge at the start of an entry, or `null` for none.
 * @property border the outline of a pending entry, or `null`.
 * @property content the label.
 */
@Immutable
internal data class TeamCalendarBarColors(
    val container: Color,
    val stripe: Color?,
    val border: Color?,
    val content: Color,
)

/** What one person has on one day. */
internal data class TeamCalendarCell(val entry: MonarchTeamCalendarEntry, val portion: MonarchTeamCalendarPortion)

/**
 * A run of days of one entry on a row, drawn as one bar. Full days of the same entry merge; a half day is a
 * segment of its own.
 *
 * @property first the index of the first day in the visible range.
 * @property count the number of days.
 * @property startsEntry whether the entry's first day is in this segment, so the bar has its start edge.
 * @property endsEntry whether the entry's last day is in this segment, so the bar has its end edge.
 */
internal data class TeamCalendarSegment(
    val entry: MonarchTeamCalendarEntry,
    val first: Int,
    val count: Int,
    val portion: MonarchTeamCalendarPortion,
    val startsEntry: Boolean,
    val endsEntry: Boolean,
)

/** Component tokens and pure logic of MonarchTeamCalendar, built from primitives and roles only. */
internal object MonarchTeamCalendarTokens {
    /** The width of a day and the smallest height of a row: the 48dp touch target. */
    val CellSize: Dp = MonarchTokens.Space.space12.dp
    val PersonWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space12).dp
    val PersonHorizontalPadding: Dp = MonarchTokens.Space.space3.dp
    val PersonVerticalPadding: Dp = MonarchTokens.Space.space1_5.dp
    val PersonGap: Dp = MonarchTokens.Space.space2.dp
    val HeaderVerticalPadding: Dp = MonarchTokens.Space.space1.dp
    val MarkSize: Dp = MonarchTokens.Space.space8.dp
    val BarMinHeight: Dp = MonarchTokens.Space.space6.dp
    val BarInset: Dp = MonarchTokens.Space.space1.dp
    val BarPadding: Dp = MonarchTokens.Space.space1_5.dp
    val StripeWidth: Dp = (MonarchTokens.Space.space0_5 + MonarchTokens.Space.px).dp
    val PlaceholderHeight: Dp = (MonarchTokens.Space.space24 * 2).dp
    val Gap: Dp = MonarchTokens.Space.space2.dp
    /** The share of the entry colour in the faded block of a pending entry. */
    const val PENDING_SHARE: Float = 0.4f
    const val TODAY_WEIGHT: Int = MonarchTokens.FontWeight.bold
    const val HEADER_WEIGHT: Int = MonarchTokens.FontWeight.medium

    /** The longest range shown, in days. */
    const val MAX_DAYS: Int = 366

    /** A half-day bar is a third of the day's width, a narrow bar as on the web. */
    const val HALF_BAR_DIVISOR: Int = 3

    /** The share of the entry colour mixed into the surface colour for the fill of a bar. */
    private const val BAR_TINT = 0.28f

    /**
     * The first and last day shown: [from] to [to] when both are set (a last day before the first gives one
     * day), otherwise the month of [month], or of [today] when [month] is `null`. At most [MAX_DAYS] days.
     */
    fun range(month: Long?, from: Long?, to: Long?, today: Long): Pair<Long, Long> {
        if (from != null && to != null) {
            val first = MonarchCalendarDates.dayOf(from)
            val last = MonarchCalendarDates.dayOf(to).coerceIn(first, first + (MAX_DAYS - 1) * DAY)
            return first to last
        }
        val first = MonarchCalendarDates.monthOf(month ?: today)
        return first to MonarchCalendarDates.addMonths(first, 1) - DAY
    }

    /** Every day from [first] to [last], inclusive. */
    fun days(first: Long, last: Long): List<Long> = List(((last - first) / DAY).toInt() + 1) { first + it * DAY }

    /** Whether [day] is shaded: a weekend day (`Calendar.SUNDAY` to `Calendar.SATURDAY`) or a holiday. */
    fun shaded(day: Long, weekendDays: Set<Int>, holidays: Map<Long, String>): Boolean =
        MonarchCalendarDates.dayOfWeek(day) in weekendDays || day in holidays

    /**
     * What each day of [days] holds for one person, from that person's [entries]. A later entry replaces an
     * earlier one on a day both cover.
     */
    fun cells(entries: List<MonarchTeamCalendarEntry>, days: List<Long>): List<TeamCalendarCell?> {
        val cells = arrayOfNulls<TeamCalendarCell>(days.size)
        if (days.isEmpty()) return cells.toList()
        val first = days.first()
        entries.forEach { entry ->
            val start = MonarchCalendarDates.dayOf(entry.start)
            val end = lastDay(entry)
            var day = maxOf(start, first)
            while (day <= end && day <= days.last()) {
                val portion = entry.portions.entries.firstOrNull { MonarchCalendarDates.dayOf(it.key) == day }?.value
                    ?: entry.portion
                cells[((day - first) / DAY).toInt()] = TeamCalendarCell(entry, portion)
                day += DAY
            }
        }
        return cells.toList()
    }

    /** The last day of [entry]: its end, or its start when the end is missing or earlier. */
    fun lastDay(entry: MonarchTeamCalendarEntry): Long {
        val start = MonarchCalendarDates.dayOf(entry.start)
        return maxOf(start, entry.end?.let(MonarchCalendarDates::dayOf) ?: start)
    }

    /** The bars of a row: runs of full days of the same entry merge, half days stand alone. */
    fun segments(cells: List<TeamCalendarCell?>, days: List<Long>): List<TeamCalendarSegment> {
        val segments = mutableListOf<TeamCalendarSegment>()
        var index = 0
        while (index < cells.size) {
            val cell = cells[index]
            if (cell == null) {
                index++
                continue
            }
            var count = 1
            if (cell.portion == MonarchTeamCalendarPortion.Full) {
                while (index + count < cells.size && cells[index + count] == cell) count++
            }
            segments += TeamCalendarSegment(
                entry = cell.entry,
                first = index,
                count = count,
                portion = cell.portion,
                startsEntry = days[index] == MonarchCalendarDates.dayOf(cell.entry.start),
                endsEntry = days[index + count - 1] == lastDay(cell.entry),
            )
            index += count
        }
        return segments
    }

    /**
     * The colours of a bar. The entry colour (the accent when unspecified) is mixed into the surface colour for
     * the fill and drawn solid as the start stripe; a pending entry has no fill and an outline in the entry
     * colour. The label keeps the default text colour, so it stays readable in every theme.
     */
    fun barColors(colors: MonarchColors, color: Color, pending: Boolean): TeamCalendarBarColors {
        val base = if (color.isSpecified) color else colors.accentSolid
        return if (pending) {
            TeamCalendarBarColors(colors.bgSurface, stripe = null, border = base, content = colors.textDefault)
        } else {
            TeamCalendarBarColors(lerp(colors.bgSurface, base, BAR_TINT), base, border = null, colors.textDefault)
        }
    }

    /** The background of a day's column: the muted fill when shaded. */
    fun columnColor(colors: MonarchColors, shaded: Boolean): Color = if (shaded) colors.bgMuted else Color.Transparent

    /** The day number in the header: today in strong accent inside an accent ring, other days muted. */
    fun headerColors(colors: MonarchColors, today: Boolean): MonarchSurfaceColors = if (today) {
        MonarchSurfaceColors(Color.Transparent, colors.accentStrong, colors.accentBorder)
    } else {
        MonarchSurfaceColors(Color.Transparent, colors.textMuted, null)
    }

    /** Spoken parts joined with commas, blanks left out. */
    fun spoken(vararg parts: String?): String = parts.filterNotNull().filter { it.isNotBlank() }.joinToString(", ")

    private const val DAY = MonarchCalendarDates.DAY_MILLIS
}
