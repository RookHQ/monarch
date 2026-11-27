package com.rookhq.monarch.components.charts.sparkline

/** The form of a `MonarchSparkline`. */
public enum class MonarchSparklineVariant {
    /** A line that breaks at gaps. The default. */
    Line,

    /** The line with a soft fill down to the baseline. */
    Area,

    /** One bar per value, from zero; negative values go down. */
    Bar,
}
