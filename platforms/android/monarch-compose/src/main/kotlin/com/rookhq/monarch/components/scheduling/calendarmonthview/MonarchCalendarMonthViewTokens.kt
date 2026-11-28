package com.rookhq.monarch.components.scheduling.calendarmonthview

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipStatus
import com.rookhq.monarch.internal.scheduling.MonarchMonthGrid
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** What a day cell of the month grid is. */
internal data class CalendarMonthViewCellState(
    val inMonth: Boolean,
    val today: Boolean,
    val selected: Boolean,
    val dropTarget: Boolean,
)

/**
 * The colours of a day cell.
 *
 * @property container the fill of the cell.
 * @property number the date number.
 * @property mark the circle behind the number; `Color.Transparent` for none.
 * @property markBorder the ring of today's number, or `null`.
 * @property outline the border of the cell while an event is dragged over it, or `null`.
 */
@Immutable
internal data class CalendarMonthViewCellColors(
    val container: Color,
    val number: Color,
    val mark: Color,
    val markBorder: Color?,
    val outline: Color?,
)

/** Component tokens of MonarchCalendarMonthView, built from primitives and roles only. */
internal object MonarchCalendarMonthViewTokens {
    /** The narrowest a day can be: the 48dp touch target. */
    val CellMinWidth: Dp = MonarchTokens.Space.space12.dp

    /** The shortest a narrow day can be: the 48dp touch target. */
    val CompactCellMinHeight: Dp = MonarchTokens.Space.space12.dp

    /** The shortest a wide day can be, room for the date and three event lines. */
    val WideCellMinHeight: Dp = MonarchTokens.Space.space24.dp

    /** From this cell width on, a day lists its events by title instead of showing dots. */
    val WideCellWidth: Dp = MonarchTokens.Space.space20.dp

    /** The circle behind the number of the selected day and today: 24dp, grows with the text. */
    val MarkSize: Dp = MonarchTokens.Space.space6.dp
    val DotSize: Dp = MonarchTokens.Space.space1_5.dp
    val DotStroke: Dp = MonarchTokens.Space.px.dp
    val DotGap: Dp = MonarchTokens.Space.space0_5.dp
    val CellPadding: Dp = MonarchTokens.Space.space0_5.dp
    val CellGap: Dp = MonarchTokens.Space.space0_5.dp
    val HatchSpacing: Dp = MonarchTokens.Space.space1_5.dp
    val GridLine: Dp = MonarchTokens.Space.px.dp
    val DropOutline: Dp = MonarchTokens.Space.space0_5.dp
    val WeekdayPadding: Dp = MonarchTokens.Space.space1.dp
    val PanelGap: Dp = MonarchTokens.Space.space2.dp
    val PanelTopPadding: Dp = MonarchTokens.Space.space3.dp
    const val TODAY_WEIGHT: Int = MonarchTokens.FontWeight.semibold

    /** The most dots a narrow cell draws; the rest are counted in "+N". */
    const val MAX_DOTS: Int = 3

    /** The width of one day when [available] is shared by seven, never under the touch target. */
    fun cellWidth(available: Dp): Dp = max(CellMinWidth, available / MonarchMonthGrid.DAYS_PER_WEEK)

    /** Whether a day [cellWidth] wide lists its events by title. */
    fun isWide(cellWidth: Dp): Boolean = cellWidth >= WideCellWidth

    /**
     * The colours of a day: the selected day's number on a solid accent
     * circle, today's in an accent ring with strong accent text, days of the
     * neighbouring months on the subtle fill with subtle text, and an accent
     * outline while an event is dragged over the day.
     */
    fun cellColors(state: CalendarMonthViewCellState, colors: MonarchColors): CalendarMonthViewCellColors =
        CalendarMonthViewCellColors(
            container = if (state.inMonth) Color.Transparent else colors.bgSubtle,
            number = when {
                state.selected -> colors.accentOnSolid
                state.today -> colors.accentStrong
                !state.inMonth -> colors.textSubtle
                else -> colors.textDefault
            },
            mark = if (state.selected) colors.accentSolid else Color.Transparent,
            markBorder = if (state.today && !state.selected) colors.accentBorder else null,
            outline = if (state.dropTarget) colors.accentSolid else null,
        )

    /** The lines between days. */
    fun gridLine(colors: MonarchColors): Color = colors.borderSubtle

    /** The diagonal lines of a hatched day. */
    fun hatch(colors: MonarchColors): Color = colors.borderDefault

    /**
     * The dot of an event in a narrow cell: the event's colour (the accent
     * when unspecified), the strong border colour for a cancelled or declined
     * event.
     */
    fun dotColor(eventColor: Color, status: MonarchEventChipStatus, colors: MonarchColors): Color = when {
        isInactive(status) -> colors.borderStrong
        eventColor.isSpecified -> eventColor
        else -> colors.accentSolid
    }

    /** Whether the dot is a ring instead of a filled circle: every status but confirmed. */
    fun isHollow(status: MonarchEventChipStatus): Boolean = status != MonarchEventChipStatus.Confirmed

    private fun isInactive(status: MonarchEventChipStatus): Boolean =
        status == MonarchEventChipStatus.Cancelled || status == MonarchEventChipStatus.Declined

    /** What TalkBack reads for a day: the date and count, then "Today" and the badge when they apply. */
    fun dayName(dateAndCount: String, today: String?, badge: String?): String =
        listOfNotNull(dateAndCount, today, badge).filter { it.isNotBlank() }.joinToString(", ")
}
