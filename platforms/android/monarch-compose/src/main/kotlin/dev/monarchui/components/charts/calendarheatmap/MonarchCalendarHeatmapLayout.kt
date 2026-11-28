package dev.monarchui.components.charts.calendarheatmap

/** How a `MonarchCalendarHeatmap` lays out its days. */
public enum class MonarchCalendarHeatmapLayout {
    /** A contribution-style year: weeks as columns and weekdays as rows, scrolling sideways. The default. */
    Year,

    /** A calendar: weekdays as columns and weeks as rows, with day numbers and values. */
    Month,
}
