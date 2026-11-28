package dev.monarchui.components.charts.uptimebar

/** The state of one period of a `MonarchUptimeBar`. Every state has a name, so colour never carries it alone. */
public enum class MonarchUptimeBarStatus {
    /** Everything worked: the success colour. */
    Operational,

    /** Slower or with some failures: the warning colour. */
    Degraded,

    /** Part of the service was down: the strong warning colour. */
    Partial,

    /** The service was down: the danger colour. */
    Down,

    /** Planned work: the info colour. */
    Maintenance,

    /** Nothing was measured: a hollow, muted segment. */
    NoData,
}
