package dev.monarchui.components.charts.linechart

/** How a `MonarchLineChart` joins neighbouring values. */
public enum class MonarchLineChartCurve {
    /** Straight lines from value to value. */
    Linear,

    /** A smooth curve that never overshoots the values (monotone cubic interpolation). */
    Monotone,
}
