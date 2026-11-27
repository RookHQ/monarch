/// Why a view is empty.
public enum MonarchEmptyStateKind: Sendable, Hashable, CaseIterable {
    /// Nothing has been created yet. The default.
    case firstRun
    /// Filters or a search hide everything.
    case filtered
    /// Everything is done, for example an empty inbox.
    case success
}
