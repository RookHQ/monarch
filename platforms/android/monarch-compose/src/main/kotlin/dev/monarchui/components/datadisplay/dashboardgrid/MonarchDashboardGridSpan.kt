package dev.monarchui.components.datadisplay.dashboardgrid

import androidx.compose.runtime.Immutable

/**
 * How many columns a widget of a `MonarchDashboardGrid` spans on wide screens.
 * A span is clamped to the columns available, so on a phone every widget
 * takes the one column.
 */
@Immutable
public class MonarchDashboardGridSpan private constructor(internal val columns: Int) {
    override fun equals(other: Any?): Boolean = other is MonarchDashboardGridSpan && other.columns == columns

    override fun hashCode(): Int = columns

    override fun toString(): String =
        if (columns == Int.MAX_VALUE) "MonarchDashboardGridSpan.Full" else "MonarchDashboardGridSpan($columns)"

    /** The common spans and a span of any number of columns. */
    public companion object {
        /** One column, the default. */
        public val One: MonarchDashboardGridSpan = MonarchDashboardGridSpan(1)

        /** Two columns. */
        public val Two: MonarchDashboardGridSpan = MonarchDashboardGridSpan(2)

        /** Every column of the row. */
        public val Full: MonarchDashboardGridSpan = MonarchDashboardGridSpan(Int.MAX_VALUE)

        /** [count] columns, at least one. */
        public fun columns(count: Int): MonarchDashboardGridSpan = MonarchDashboardGridSpan(count.coerceAtLeast(1))
    }
}
