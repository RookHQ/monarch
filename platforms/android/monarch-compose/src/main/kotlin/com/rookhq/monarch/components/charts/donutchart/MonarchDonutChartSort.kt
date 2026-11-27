package com.rookhq.monarch.components.charts.donutchart

/** The order of the slices of a `MonarchDonutChart`, clockwise from the top (anticlockwise right to left). */
public enum class MonarchDonutChartSort {
    /** The largest slice first; equal slices keep their given order. The default. */
    Descending,

    /** The order the segments were given in. */
    None,
}
