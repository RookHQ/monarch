package com.rookhq.monarch.components.workflow.timesheetgrid

import androidx.compose.ui.unit.Dp

/** Which layout a MonarchTimesheetGrid shows and how wide its first column is. */
internal object TimesheetGridLayoutRules {
    private val tokens = MonarchTimesheetGridTokens

    /** The width the grid's columns other than the row names take. */
    fun fixedColumnsWidth(dayCount: Int): Dp = tokens.DayColumnWidth * dayCount + tokens.TotalColumnWidth

    /** Whether the automatic layout shows the grid: every column fits [width] and the text is not enlarged much. */
    fun fitsGrid(width: Dp, dayCount: Int, fontScale: Float): Boolean =
        fontScale <= tokens.GRID_MAX_FONT_SCALE && width >= tokens.RowHeaderMinWidth + fixedColumnsWidth(dayCount)

    /** The width of the row names column in [width]: the rest of the row, at least its minimum. */
    fun rowHeaderWidth(width: Dp, dayCount: Int): Dp =
        maxOf(tokens.RowHeaderMinWidth, width - fixedColumnsWidth(dayCount))

    /** Which layout to show. */
    fun resolve(layout: MonarchTimesheetGridLayout, fits: Boolean): MonarchTimesheetGridLayout = when (layout) {
        MonarchTimesheetGridLayout.Automatic ->
            if (fits) MonarchTimesheetGridLayout.Grid else MonarchTimesheetGridLayout.Day
        else -> layout
    }
}
