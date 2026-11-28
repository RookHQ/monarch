package dev.monarchui.components.charts.deltaindicator

/** Which direction of change is an improvement, for a `MonarchDeltaIndicator`. */
public enum class MonarchDeltaIndicatorGoodDirection {
    /** An increase is better, as for revenue or sign-ups. The default. */
    Up,

    /** A decrease is better, as for costs, latency or churn. */
    Down,

    /** Neither direction is better; the change is shown in the muted colour. */
    Neither,
}
