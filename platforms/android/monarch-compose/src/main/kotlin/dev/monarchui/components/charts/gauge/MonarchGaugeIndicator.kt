package dev.monarchui.components.charts.gauge

/** How a `MonarchGauge` shows its value. */
public enum class MonarchGaugeIndicator {
    /** The track is filled from the start to the value. The default. */
    Arc,

    /** The bands are drawn on the track and a needle points at the value. */
    Needle,
}
