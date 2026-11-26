package com.rookhq.monarch.components.files.tableblock

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The position of a cell, counted from 0. */
internal data class MonarchTableBlockCell(val row: Int, val column: Int)

/** Component tokens of MonarchTableBlock, built from primitives and roles only. */
internal object MonarchTableBlockTokens {
    /** The least width of a column of a read-only table, at the default font size. */
    val MinColumnWidth: Dp = MonarchTokens.Space.space24.dp

    /** The width at which the text of a read-only column wraps, at the default font size. */
    val MaxColumnWidth: Dp = MonarchTokens.Space.space24.dp * 2.5f

    /** The width of every column of an editable table, at the default font size. */
    val EditableColumnWidth: Dp = MonarchTokens.Space.space24.dp * 1.5f

    /** Padding before and after the text of a cell. */
    val CellHorizontalPadding: Dp = MonarchTokens.Space.space2.dp

    /** Padding above and below the text of a cell. */
    val CellVerticalPadding: Dp = MonarchTokens.Space.space1_5.dp

    /** The least height of an editable cell, which is its touch target. */
    val EditableCellMinHeight: Dp = MonarchTouchTarget.Minimum

    /** The gap between the controls, the caption and the table. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The largest factor by which column widths grow with the font size setting. */
    const val MAX_WIDTH_SCALE: Float = 2f

    /** The colour of the lines between cells and around the table. */
    fun line(colors: MonarchColors): Color = colors.borderDefault

    /** The colour of the caption. */
    fun caption(colors: MonarchColors): Color = colors.textMuted

    /** The fill of a cell: header cells sit on the subtle background. */
    fun background(header: Boolean, colors: MonarchColors): Color = if (header) colors.bgSubtle else colors.bgSurface

    /** The colour of the text of a cell. */
    fun foreground(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textDefault else colors.textDisabled

    /** How much column widths grow with [fontScale]: as much as the text, at most twice. */
    fun widthScale(fontScale: Float): Float = fontScale.coerceIn(1f, MAX_WIDTH_SCALE)

    /** The spoken name of the table: the label, else the caption, else [fallback]. */
    fun regionLabel(label: String?, caption: String?, fallback: String): String =
        label?.takeIf { it.isNotEmpty() } ?: caption?.takeIf { it.isNotEmpty() } ?: fallback

    /** Whether the visible caption is hidden from TalkBack because it is already the name of the table. */
    fun captionIsSpokenName(label: String?): Boolean = label.isNullOrEmpty()

    /** What TalkBack reads for a read-only cell: its text, or [empty] when it has none. */
    fun cellLabel(text: String, empty: String): String = text.ifBlank { empty }

    /**
     * The cell the controls act on: the cell that last had focus if it is
     * still inside the table, otherwise the nearest cell; the last cell when
     * none had focus.
     */
    fun target(active: MonarchTableBlockCell?, data: MonarchTableBlockData): MonarchTableBlockCell {
        val last = MonarchTableBlockCell(data.rowCount - 1, data.columnCount - 1)
        if (active == null || active.row < 0 || active.column < 0) return last
        return MonarchTableBlockCell(minOf(active.row, last.row), minOf(active.column, last.column))
    }
}
