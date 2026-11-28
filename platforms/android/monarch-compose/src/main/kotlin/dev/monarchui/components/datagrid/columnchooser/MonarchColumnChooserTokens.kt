package dev.monarchui.components.datagrid.columnchooser

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure rules of MonarchColumnChooser. */
internal object MonarchColumnChooserTokens {
    /** The space between the header, the search field, the list and the footer. */
    val SectionGap: Dp = MonarchTokens.Space.space3.dp

    /** The space between rows that cannot be reordered. */
    val RowGap: Dp = MonarchTokens.Space.space1.dp

    /** The space between the footer's buttons. */
    val FooterGap: Dp = MonarchTokens.Space.space2.dp

    /** The colour of the count under the title. */
    fun countColor(colors: MonarchColors): Color = colors.textMuted

    /** The columns in display order: those in [order] first, as listed, then the rest in their default order. */
    fun ordered(columns: List<MonarchColumnChooserColumn>, order: List<String>): List<MonarchColumnChooserColumn> {
        if (order.isEmpty()) return columns
        val position = order.withIndex().associate { (index, id) -> id to index }
        return columns.withIndex()
            .sortedBy { (index, column) -> position[column.id] ?: (order.size + index) }
            .map { it.value }
    }

    /** Whether [column] is shown: a column that cannot be hidden always is. */
    fun isShown(column: MonarchColumnChooserColumn, hidden: Set<String>): Boolean =
        !column.hideable || column.id !in hidden

    /** Whether the person can switch [column]: it can be hidden, and it is not one of the last shown. */
    fun canToggle(
        column: MonarchColumnChooserColumn,
        hidden: Set<String>,
        shownCount: Int,
        minimumVisible: Int,
    ): Boolean =
        column.hideable && !(isShown(column, hidden) && shownCount <= minimumVisible)

    /** The hidden set after switching [column] [on]. */
    fun toggled(hidden: Set<String>, column: MonarchColumnChooserColumn, on: Boolean): Set<String> =
        if (on) hidden - column.id else hidden + column.id

    /** The columns whose label contains [query], ignoring case and surrounding space; all of them for none. */
    fun matching(columns: List<MonarchColumnChooserColumn>, query: String): List<MonarchColumnChooserColumn> {
        val needle = query.trim()
        if (needle.isEmpty()) return columns
        return columns.filter { it.label.contains(needle, ignoreCase = true) }
    }

    /** A copy of [ids] with the id at [from] moved to [to], clamped to the list. */
    fun moved(ids: List<String>, from: Int, to: Int): List<String> {
        if (from !in ids.indices) return ids
        return ids.toMutableList().apply { add(to.coerceIn(0, ids.lastIndex), removeAt(from)) }
    }
}
