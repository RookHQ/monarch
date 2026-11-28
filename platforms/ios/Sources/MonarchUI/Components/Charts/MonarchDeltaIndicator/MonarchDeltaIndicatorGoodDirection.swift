/// Which direction of a change is an improvement.
public enum MonarchDeltaIndicatorGoodDirection: Sendable, Hashable, CaseIterable {
    /// More is better: revenue, sign-ups. The default.
    case up
    /// Less is better: cost, latency, churn.
    case down
    /// No direction is better. The change is drawn without a status colour.
    case neither
}
