package com.rookhq.monarch.components.charts.burndownchart

/** A line of a `MonarchBurndownChart`, as kept in its hidden set. */
public enum class MonarchBurndownChartLine {
    /** The work left at the end of each day. */
    Remaining,

    /** The straight line from the starting work to zero on the last day. */
    Ideal,

    /** The committed work each day, drawn as a step line with its changes. */
    Scope,
}
