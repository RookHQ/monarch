/// Replacements for the built-in strings of ``MonarchSearchResults``.
///
/// A property that is `nil` keeps the built-in English string.
public struct MonarchSearchResultsLabels: Sendable {
    /// The summary when results are shown. Receives the total and the query.
    /// Built in: "12 results for “travel”", or "12 results" without a query.
    public var summary: (@Sendable (Int, String) -> String)?
    /// The summary while searching. Built in: "Searching…".
    public var loading: String?
    /// The empty state's title. Receives the query. Built in: "No results
    /// for “travel”", or "No results" without a query.
    public var emptyTitle: (@Sendable (String) -> String)?
    /// The empty state's message. Built in: "Try other words, check the
    /// spelling, or remove a filter."
    public var emptyMessage: String?
    /// The error state's title. Built in: "Search failed".
    public var errorTitle: String?
    /// The retry button. Built in: the error state's "Try again".
    public var retry: String?
    /// The button after a group with more matches. Receives the group's
    /// heading and total. Built in: "Show all 24 in Documents".
    public var showAll: (@Sendable (String?, Int) -> String)?
    /// The count read with a group heading. Receives the total. Built in:
    /// "24 results".
    public var groupCount: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        summary: (@Sendable (Int, String) -> String)? = nil,
        loading: String? = nil,
        emptyTitle: (@Sendable (String) -> String)? = nil,
        emptyMessage: String? = nil,
        errorTitle: String? = nil,
        retry: String? = nil,
        showAll: (@Sendable (String?, Int) -> String)? = nil,
        groupCount: (@Sendable (Int) -> String)? = nil
    ) {
        self.summary = summary
        self.loading = loading
        self.emptyTitle = emptyTitle
        self.emptyMessage = emptyMessage
        self.errorTitle = errorTitle
        self.retry = retry
        self.showAll = showAll
        self.groupCount = groupCount
    }
}
