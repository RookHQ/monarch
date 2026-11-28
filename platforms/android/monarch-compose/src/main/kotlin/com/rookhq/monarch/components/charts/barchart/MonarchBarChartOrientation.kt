package com.rookhq.monarch.components.charts.barchart

/** The direction of the bars of a `MonarchBarChart`. */
public enum class MonarchBarChartOrientation {
    /** Columns that grow up from the zero line; categories run from the start edge to the end edge. */
    Vertical,

    /** Bars that grow towards the end edge; categories run from top to bottom. For long names and rankings. */
    Horizontal,
}
