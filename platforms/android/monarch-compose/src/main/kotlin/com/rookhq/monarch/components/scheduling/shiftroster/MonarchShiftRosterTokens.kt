package com.rookhq.monarch.components.scheduling.shiftroster

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * The colours of a cell that has no shift.
 *
 * @property container the fill.
 * @property content the text.
 * @property border the dashed outline, or `null` for none.
 */
@Immutable
internal data class ShiftRosterSlotColors(val container: Color, val content: Color, val border: Color?)

/** Component tokens and layout rules of MonarchShiftRoster, built from primitives and roles only. */
internal object MonarchShiftRosterTokens {
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val RowPadding: Dp = MonarchTokens.Space.space2.dp
    val RowGap: Dp = MonarchTokens.Space.space3.dp
    val PersonGap: Dp = MonarchTokens.Space.space3.dp
    val SummaryGap: Dp = MonarchTokens.Space.space2.dp
    val SlotMinHeight: Dp = MonarchTokens.Space.space8.dp
    val SlotHorizontalPadding: Dp = MonarchTokens.Space.space2.dp
    val SlotVerticalPadding: Dp = MonarchTokens.Space.space1.dp
    val DashLength: Dp = MonarchTokens.Space.space1.dp
    val CellPadding: Dp = MonarchTokens.Space.space1.dp
    val HeaderPadding: Dp = MonarchTokens.Space.space2.dp
    val PlaceholderHeight: Dp = (MonarchTokens.Space.space24 * 2).dp

    /** The narrowest the people column of the grid gets. */
    val PersonColumnMinWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space16).dp

    /** The width of a day column of the grid. */
    val DayColumnWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space4).dp

    /** The width of the hours column of the grid. */
    val HoursColumnWidth: Dp = MonarchTokens.Space.space20.dp

    /** The largest font scale at which the automatic layout still shows the grid. */
    const val GRID_MAX_FONT_SCALE: Float = 1.3f

    /** From this font scale a row puts the cell under the person or day instead of beside it. */
    const val STACK_FONT_SCALE: Float = 1.5f

    /** The visible mark of a day with no assignment in the narrow cells of the grid. */
    const val UNASSIGNED_MARK: String = "–"

    /** The width the grid's columns other than the people column take. */
    fun fixedColumnsWidth(dayCount: Int, totals: Boolean): Dp =
        DayColumnWidth * dayCount + if (totals) HoursColumnWidth else MonarchTokens.Space.space0.dp

    /** Whether the automatic layout shows the grid: every column fits [width] and the text is not enlarged much. */
    fun fitsGrid(width: Dp, dayCount: Int, totals: Boolean, fontScale: Float): Boolean =
        fontScale <= GRID_MAX_FONT_SCALE && width >= PersonColumnMinWidth + fixedColumnsWidth(dayCount, totals)

    /** The width of the people column in [width]: the rest of the row, at least its minimum. */
    fun personColumnWidth(width: Dp, dayCount: Int, totals: Boolean): Dp =
        maxOf(PersonColumnMinWidth, width - fixedColumnsWidth(dayCount, totals))

    /** Which layout to show. */
    fun resolve(layout: MonarchShiftRosterLayout, fits: Boolean): MonarchShiftRosterLayout = when (layout) {
        MonarchShiftRosterLayout.Automatic ->
            if (fits) MonarchShiftRosterLayout.Grid else MonarchShiftRosterLayout.ByDay
        else -> layout
    }

    /** The day tab that opens first: today when it is in [range], otherwise the first day. */
    fun initialIndex(range: List<Long>, today: Long?): Int = range.indexOf(today).coerceAtLeast(0)

    /**
     * A cell without a shift: a day off is a muted fill with muted text; a day that is not assigned is a
     * dashed outline in the default border colour on no fill. Both also say what they are in words.
     */
    fun slotColors(colors: MonarchColors, off: Boolean): ShiftRosterSlotColors = if (off) {
        ShiftRosterSlotColors(container = colors.bgMuted, content = colors.textMuted, border = null)
    } else {
        ShiftRosterSlotColors(container = Color.Transparent, content = colors.textMuted, border = colors.borderDefault)
    }

    /** The colour of a day's name: always the text colour. Today is marked by its badge and tab, not by an accent. */
    fun dayColor(colors: MonarchColors): Color = colors.textDefault

    /** The fill of the grid's header and footer rows. */
    fun headerContainer(colors: MonarchColors): Color = colors.bgSubtle

    /** The colour of descriptions, totals and counts. */
    fun muted(colors: MonarchColors): Color = colors.textMuted
}
