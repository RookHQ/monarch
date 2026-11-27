package com.rookhq.monarch.components.charts.linechart

/** How a `MonarchLineChart` draws each series. */
public enum class MonarchLineChartVariant {
    /** A line through the values. */
    Line,

    /** The line with a soft fill of its colour down to zero, or to the bottom of the axis when zero is left out. */
    Area,
}
