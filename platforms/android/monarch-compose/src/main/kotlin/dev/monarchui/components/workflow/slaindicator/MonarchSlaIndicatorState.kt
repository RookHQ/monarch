package dev.monarchui.components.workflow.slaindicator

/** The state of a service-level target. The same six names exist on every platform. */
public enum class MonarchSlaIndicatorState {
    /** Running, with time to spare. */
    OnTrack,

    /** Running, and the due time is close. */
    AtRisk,

    /** The due time has passed without the target being met. */
    Breached,

    /** The clock is stopped, for example while waiting on the customer. */
    Paused,

    /** The target was achieved in time. */
    Met,

    /** No target applies. The web calls it none. */
    NotApplicable,
}
