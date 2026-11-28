package dev.monarchui.components.charts.donutchart

/** What each legend entry of a `MonarchDonutChart` shows after its label. */
public enum class MonarchDonutChartLegendContent {
    /** The value and the share: "412 · 47%". The default. */
    Both,

    /** The value only. */
    Value,

    /** The share only. */
    Percent,

    /** The label only. */
    None,
}
