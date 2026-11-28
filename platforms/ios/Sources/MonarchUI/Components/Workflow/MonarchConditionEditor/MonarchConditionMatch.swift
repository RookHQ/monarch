/// How the conditions of a ``MonarchConditionEditor`` combine.
public enum MonarchConditionMatch: String, Hashable, Sendable, CaseIterable {
    /// Every condition must hold.
    case all
    /// At least one condition must hold.
    case any
}
