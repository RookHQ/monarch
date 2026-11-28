/// How the times of a ``MonarchEscalationPolicyEditor`` are measured.
public enum MonarchEscalationPolicyTiming: Sendable, Hashable, CaseIterable {
    /// Every time is measured from the start, so each level must be later
    /// than the one before. The default.
    case cumulative
    /// Each time is measured from the previous level.
    case interval
}
