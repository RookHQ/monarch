/// How the rules and groups of a ``MonarchQueryGroup`` combine.
public enum MonarchQueryCombinator: String, Hashable, Sendable, CaseIterable {
    /// Every rule and group must hold.
    case and
    /// At least one rule or group must hold.
    case or
}
