package dev.monarchui.components.charts.chartlegend

/** The mark drawn before each legend entry. Use the shape the chart draws. */
public enum class MonarchChartLegendMarker {
    /** A short line, for line charts. Dashed entries draw it dashed. */
    Line,

    /** A small square, for bars and areas. Dashed entries draw it outlined. */
    Square,

    /** A dot, for scatter plots and points. */
    Dot,
}
