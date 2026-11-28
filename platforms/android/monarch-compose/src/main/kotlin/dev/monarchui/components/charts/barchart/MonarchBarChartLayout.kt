package dev.monarchui.components.charts.barchart

/** How a `MonarchBarChart` with several series places the bars of one category. */
public enum class MonarchBarChartLayout {
    /** Side by side, one bar per series. */
    Grouped,

    /** One bar per category, the series stacked from zero; negative values stack the other way. */
    Stacked,
}
