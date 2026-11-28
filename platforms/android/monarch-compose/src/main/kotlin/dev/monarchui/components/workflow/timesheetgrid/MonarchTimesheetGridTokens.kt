package dev.monarchui.components.workflow.timesheetgrid

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens, colour decisions and layout rules of MonarchTimesheetGrid, from primitives and roles only. */
internal object MonarchTimesheetGridTokens {
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val LineGap: Dp = MonarchTokens.Space.space0_5.dp
    val HeaderPadding: Dp = MonarchTokens.Space.space2.dp
    val CellPadding: Dp = MonarchTokens.Space.space1.dp
    val FieldPadding: Dp = MonarchTokens.Space.space2.dp
    val RowPadding: Dp = MonarchTokens.Space.space2.dp
    val RowGap: Dp = MonarchTokens.Space.space3.dp
    val TodayBarHeight: Dp = MonarchTokens.Space.space0_5.dp

    /** The narrowest the row names column of the grid gets. */
    val RowHeaderMinWidth: Dp =
        (MonarchTokens.Space.space24 + MonarchTokens.Space.space24 + MonarchTokens.Space.space8).dp

    /** The width of a day column of the grid; fits "10h 30m". */
    val DayColumnWidth: Dp = (MonarchTokens.Space.space20 + MonarchTokens.Space.space2).dp

    /** The width of the total column of the grid. */
    val TotalColumnWidth: Dp = (MonarchTokens.Space.space20 + MonarchTokens.Space.space2).dp

    /** The largest font scale at which the automatic layout still shows the grid. */
    const val GRID_MAX_FONT_SCALE: Float = 1.3f

    /** The visible mark of an empty cell or total. */
    const val EMPTY_MARK: String = "–"

    /** The colour of a day's name: the strong accent for today, which is also said in words. */
    fun dayColor(colors: MonarchColors, today: Boolean): Color = if (today) colors.accentStrong else colors.textDefault

    /** The bar under today's heading. */
    fun todayBar(colors: MonarchColors): Color = colors.accentSolid

    /** The fill of a day's column: shaded when the day is locked or not a working day. */
    fun columnContainer(colors: MonarchColors, locked: Boolean, nonWorking: Boolean): Color =
        if (locked || nonWorking) colors.bgSubtle else Color.Transparent

    /** The colour of a read-only value: muted on a locked day. */
    fun valueColor(colors: MonarchColors, locked: Boolean): Color =
        if (locked) colors.textMuted else colors.textDefault

    /** The colour of a day total: the strong danger colour above the daily maximum, also said in words. */
    fun totalColor(colors: MonarchColors, over: Boolean): Color = if (over) colors.dangerStrong else colors.textDefault

    /** The fill of the heading and footer rows of the grid. */
    fun headerContainer(colors: MonarchColors): Color = colors.bgSubtle

    /** The colour of descriptions, notes, expected times and summary rows. */
    fun muted(colors: MonarchColors): Color = colors.textMuted

    /** The colour of an error message. */
    fun error(colors: MonarchColors): Color = colors.dangerStrong
}
