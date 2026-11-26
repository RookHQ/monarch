/// What happens to something when an action runs.
public enum MonarchConsequenceKind: Sendable, Hashable, CaseIterable {
    /// It is deleted or lost.
    case removed
    /// It is altered or moved.
    case changed
    /// Something to know before continuing.
    case warning
    /// It stays as it is.
    case kept
}
