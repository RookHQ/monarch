/// The state of one period of a ``MonarchUptimeBar``.
///
/// Status colours carry status here, never a series, and every state also
/// has a spoken and written name.
public enum MonarchUptimeBarStatus: Sendable, Hashable, CaseIterable {
    /// Everything worked.
    case operational
    /// Slower or less reliable than usual.
    case degraded
    /// Part of the service was down.
    case partial
    /// The service was down.
    case down
    /// Planned maintenance.
    case maintenance
    /// Nothing was measured. Drawn hollow, so it never reads as a state.
    case noData
}
