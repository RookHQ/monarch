/// The state of a service-level target.
public enum MonarchSlaIndicatorState: Sendable, Hashable, CaseIterable {
    /// Running, with time to spare.
    case onTrack
    /// Running, and the due time is close.
    case atRisk
    /// The due time has passed without the target being met.
    case breached
    /// The clock is stopped, for example while waiting on the customer.
    case paused
    /// The target was achieved in time.
    case met
    /// No target applies.
    case notApplicable
}
