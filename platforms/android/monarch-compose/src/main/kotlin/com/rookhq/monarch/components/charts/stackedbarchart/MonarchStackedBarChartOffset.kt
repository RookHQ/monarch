package com.rookhq.monarch.components.charts.stackedbarchart

/** How a stacked `MonarchStackedBarChart` scales its bars. */
public enum class MonarchStackedBarChartOffset {
    /** The parts stack their values, so bars differ in length with their totals. The default. */
    Normal,

    /** Each category's parts stack as shares of its total, so every bar is 100% long. */
    Expand,
}
