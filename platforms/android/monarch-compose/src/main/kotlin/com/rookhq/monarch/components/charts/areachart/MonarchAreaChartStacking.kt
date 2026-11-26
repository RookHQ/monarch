package com.rookhq.monarch.components.charts.areachart

/** How the areas of a `MonarchAreaChart` combine. */
public enum class MonarchAreaChartStacking {
    /** Each series fills from the baseline; the areas overlap and every top line stays visible. */
    Overlap,

    /** The series pile up in order, the first at the bottom, so the top edge is the total. */
    Stacked,

    /** The series pile up as shares of each x value's total, from 0% to 100%. */
    Expand,
}
