package com.rookhq.monarch.components.datadisplay.descriptionlist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchNumberText
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure rules of MonarchDescriptionList. */
internal object MonarchDescriptionListTokens {
    /** The space between rows of items. */
    val RowGap: Dp = MonarchTokens.Space.space4.dp

    /** The space between columns, and between the term and the value of an inline item. */
    val ColumnGap: Dp = MonarchTokens.Space.space4.dp

    /** The space between the term and the value of a stacked item. */
    val TermGap: Dp = MonarchTokens.Space.space1.dp

    /** Below this width every item takes a whole row; a phone shows the columns the web example shows. */
    val ColumnsMinWidth: Dp = (MonarchTokens.Space.space24 * COLUMNS_MIN_STEPS).dp

    /** Below this width an inline list is stacked. */
    val InlineMinWidth: Dp = (MonarchTokens.Space.space24 * INLINE_STEPS).dp

    /** From this font scale an inline list is stacked, so long terms do not squeeze the value. */
    const val INLINE_MAX_FONT_SCALE: Float = 1.5f

    /** The share of the row width the term of an inline item takes. */
    const val INLINE_TERM_WEIGHT: Float = 2f

    /** The share of the row width the value of an inline item takes. */
    const val INLINE_VALUE_WEIGHT: Float = 3f

    /** The text of an empty value. TalkBack reads the empty label instead. */
    const val EMPTY_TEXT: String = MonarchNumberText.EMPTY_MARK

    private const val INLINE_STEPS = 3f
    private const val COLUMNS_MIN_STEPS = 3f

    /** Whether a value is empty and shows the dash. */
    fun isEmpty(item: MonarchDescriptionItem): Boolean = item.content == null && item.value.isNullOrEmpty()

    /** The text the copy button copies, or `null` when the item has no copy button. */
    fun copyText(item: MonarchDescriptionItem): String? = when {
        isEmpty(item) -> null
        item.copyValue != null -> item.copyValue
        item.copyable -> item.value?.takeIf { it.isNotEmpty() }
        else -> null
    }

    /** The number of columns at [widthDp]: one below the breakpoint, else the requested number. */
    fun columns(requested: Int, widthDp: Float): Int =
        if (widthDp < ColumnsMinWidth.value) 1 else requested.coerceAtLeast(1)

    /** The layout at [widthDp] and [fontScale]: inline only when there is room for it. */
    fun layout(
        requested: MonarchDescriptionListLayout,
        widthDp: Float,
        fontScale: Float,
    ): MonarchDescriptionListLayout =
        if (requested == MonarchDescriptionListLayout.Inline &&
            widthDp >= InlineMinWidth.value &&
            fontScale < INLINE_MAX_FONT_SCALE
        ) {
            MonarchDescriptionListLayout.Inline
        } else {
            MonarchDescriptionListLayout.Stacked
        }

    /** The items, as indexes, row by row: [columns] per row, and a wide item alone on its row. */
    fun rows(items: List<MonarchDescriptionItem>, columns: Int): List<List<Int>> {
        val rows = mutableListOf<List<Int>>()
        var current = mutableListOf<Int>()
        items.forEachIndexed { index, item ->
            if (item.wide || columns <= 1) {
                if (current.isNotEmpty()) rows += current
                current = mutableListOf()
                rows += listOf(index)
            } else {
                current += index
                if (current.size == columns) {
                    rows += current
                    current = mutableListOf()
                }
            }
        }
        if (current.isNotEmpty()) rows += current
        return rows
    }

    /** The colour of a term. */
    fun termColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of a value: the default text colour, or the subtle one for the empty dash. */
    fun valueColor(empty: Boolean, colors: MonarchColors): Color = if (empty) colors.textSubtle else colors.textDefault
}
