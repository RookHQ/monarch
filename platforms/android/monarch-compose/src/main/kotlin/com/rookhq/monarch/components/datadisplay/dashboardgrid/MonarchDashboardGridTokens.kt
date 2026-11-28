package com.rookhq.monarch.components.datadisplay.dashboardgrid

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure layout rules of MonarchDashboardGrid. */
internal object MonarchDashboardGridTokens {
    /** The space between widgets, across and down. */
    val Gap: Dp = MonarchTokens.Space.space4.dp

    /** The space inside the arrange bar. */
    val BarPadding: Dp = MonarchTokens.Space.space2.dp

    /** The space between the parts of the arrange bar. */
    val BarGap: Dp = MonarchTokens.Space.space1.dp

    /** From this grid width two columns are allowed. */
    val MediumWidth: Dp = MonarchTokens.Breakpoint.sm.dp

    /** From this grid width the full [MAX_COLUMNS] the app asks for are allowed. */
    val WideWidth: Dp = MonarchTokens.Breakpoint.lg.dp

    /** From this font scale the grid has one column, so large text is not squeezed. */
    const val LARGE_TEXT_SCALE: Float = 1.5f

    /** The most columns a grid can have. */
    const val MAX_COLUMNS: Int = 4

    /** The columns at [width]: one below [MediumWidth] or with large text, at most two below [WideWidth]. */
    fun columnsAt(width: Dp, fontScale: Float, columns: Int): Int {
        val wanted = columns.coerceIn(1, MAX_COLUMNS)
        return when {
            fontScale >= LARGE_TEXT_SCALE || width < MediumWidth -> 1
            width < WideWidth -> minOf(2, wanted)
            else -> wanted
        }
    }

    /** The columns [span] takes in a row of [available] columns. */
    fun spanOf(span: MonarchDashboardGridSpan, available: Int): Int =
        span.columns.coerceIn(1, available.coerceAtLeast(1))

    /**
     * The order of the widgets from a stored [layout]: ids that no longer
     * exist are skipped, repeats are dropped, and widgets missing from it
     * follow in their default order.
     */
    fun resolve(ids: List<String>, layout: List<String>): List<String> {
        val known = ids.toSet()
        val seen = LinkedHashSet<String>()
        layout.filterTo(seen) { it in known }
        ids.filterTo(seen) { it !in seen }
        return seen.toList()
    }

    /** A copy of [ids] with the id at [from] moved to [to], clamped to the list. */
    fun moved(ids: List<String>, from: Int, to: Int): List<String> {
        if (from !in ids.indices) return ids
        return ids.toMutableList().apply { add(to.coerceIn(0, ids.lastIndex), removeAt(from)) }
    }
}
