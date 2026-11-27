/// Replacements for the built-in strings of ``MonarchScopeSelector``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchScopeSelectorLabels: Sendable {
    /// Shown when there are no groups. Built in: "There are no scopes to choose from."
    public var empty: String?
    /// A group's summary while its scopes load. Built in: "Loading scopes…".
    public var loading: String?
    /// A group's summary when it offers no scopes. Built in: "No scopes available."
    public var noScopes: String?
    /// A group's summary from the selected and total counts. Built in: "1 of 2 scopes".
    public var count: (@Sendable (Int, Int) -> String)?
    /// The select-all checkbox of a group. Built in: "Select all".
    public var selectAll: String?
    /// The paging button, from how many it shows and how many are hidden. Built in: "Show 10 more of 25".
    public var showMore: (@Sendable (Int, Int) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    ///
    /// - Parameters:
    ///   - empty: Shown when there are no groups.
    ///   - loading: A group's summary while its scopes load.
    ///   - noScopes: A group's summary when it offers no scopes.
    ///   - count: A group's summary from the selected and total counts.
    ///   - selectAll: The select-all checkbox of a group.
    ///   - showMore: The paging button, from how many it shows and how many are hidden.
    public init(
        empty: String? = nil,
        loading: String? = nil,
        noScopes: String? = nil,
        count: (@Sendable (Int, Int) -> String)? = nil,
        selectAll: String? = nil,
        showMore: (@Sendable (Int, Int) -> String)? = nil
    ) {
        self.empty = empty
        self.loading = loading
        self.noScopes = noScopes
        self.count = count
        self.selectAll = selectAll
        self.showMore = showMore
    }
}
