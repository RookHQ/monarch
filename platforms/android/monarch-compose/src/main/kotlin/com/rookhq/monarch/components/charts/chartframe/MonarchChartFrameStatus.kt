package com.rookhq.monarch.components.charts.chartframe

/** What the body of a `MonarchChartFrame` shows. Every status keeps the chart's height. */
public enum class MonarchChartFrameStatus {
    /** The chart, its legend and, on request, its data table. */
    Ready,

    /** A skeleton of the chart's size, read as "Loading chart". */
    Loading,

    /** An empty state: "No data yet" and the empty message. */
    Empty,

    /** The error message, with Try again when the frame has `onRetry`. */
    Error,
}
