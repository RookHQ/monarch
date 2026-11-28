package com.rookhq.monarch.components.intelligence.airesponseskeleton

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure layout rules of MonarchAiResponseSkeleton. */
internal object MonarchAiResponseSkeletonTokens {
    /** The gap between text lines, cards and table rows. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between table cells and between the lines inside a card. */
    val InnerGap: Dp = MonarchTokens.Space.space2.dp

    /** The padding inside a card outline. */
    val CardPadding: Dp = MonarchTokens.Space.space3.dp

    /** The padding above and below a table row. */
    val RowPadding: Dp = MonarchTokens.Space.space1_5.dp

    /** Lines, cards or body rows when the caller gives none: 3, 3 and 4, as on the web. */
    fun defaultCount(shape: MonarchAiResponseSkeletonShape): Int = when (shape) {
        MonarchAiResponseSkeletonShape.Text, MonarchAiResponseSkeletonShape.Cards -> DEFAULT_COUNT
        MonarchAiResponseSkeletonShape.Table -> DEFAULT_TABLE_ROWS
    }

    /** The number drawn: the caller's, at least one, or the default of the shape. */
    fun count(shape: MonarchAiResponseSkeletonShape, lines: Int?): Int = maxOf(1, lines ?: defaultCount(shape))

    /** The number of table columns drawn: at least one. */
    fun columns(columns: Int): Int = maxOf(1, columns)

    /**
     * The width of text line [index] of [count], as a fraction of the available width. Fixed
     * widths, as on the web; the last of several lines is shorter.
     */
    fun lineWidth(index: Int, count: Int): Float =
        if (count > 1 && index == count - 1) LAST_LINE else WIDTHS[index % WIDTHS.size]

    /** The width of a body cell, as a fraction of the cell: the web's widths, offset by row and column. */
    fun cellWidth(row: Int, column: Int): Float = lineWidth((row + column) % CELL_CYCLE, CELL_CYCLE + 1)

    /** The widths of the three lines of a card: a title line and two lines of text. */
    val CardLines: List<Float> = listOf(CARD_TITLE, 1f, CARD_LAST)

    /** The width of a header cell, as a fraction of the cell. */
    const val HEADER_WIDTH: Float = 0.6f

    /** The outline of a card and the rule under the table header: the surface fill with the subtle border. */
    fun cardColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(container = colors.bgSurface, content = colors.textDefault, border = colors.borderSubtle)


    private const val DEFAULT_COUNT = 3
    private const val DEFAULT_TABLE_ROWS = 4
    private const val CELL_CYCLE = 5
    private const val LAST_LINE = 0.62f
    private const val CARD_TITLE = 0.55f
    private const val CARD_LAST = 0.78f
    private val WIDTHS = listOf(1f, 0.94f, 0.97f, 0.88f, 0.91f, 0.96f, 0.85f)
}
