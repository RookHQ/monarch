/// Which groups of a ``MonarchScopeSelector`` get every scope selected when they load with none
/// selected, so a grant never ends up with no access by accident.
public enum MonarchScopeSelectorAutoSelect: Hashable, Sendable, CaseIterable {
    /// Every group, once, as soon as its scopes have loaded.
    case all
    /// Only groups added after the selector first appears, for editing an existing grant.
    case added
}
