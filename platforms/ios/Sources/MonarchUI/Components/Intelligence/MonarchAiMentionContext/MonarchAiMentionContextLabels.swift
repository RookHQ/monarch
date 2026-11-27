/// Replacements for the built-in strings of ``MonarchAiMentionContext``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiMentionContextLabels: Sendable {
    /// Names the list. Built in: "Add context".
    public var label: String?
    /// The heading of a kind, shown when results have more than one kind.
    /// Built in: "Documents", "People", "Records", "Apps".
    public var group: (@Sendable (MonarchAiMentionKind) -> String)?
    /// Read while searching. Built in: "Searching…".
    public var loading: String?
    /// Shown when nothing matches, from the query. Built in: "Type to search"
    /// for an empty query, else "Nothing matches “report”".
    public var empty: (@Sendable (String) -> String)?
    /// Shown when the search failed. Built in: "Couldn’t load results."
    public var error: String?
    /// The retry button. Built in: the inline error's "Try again".
    public var retry: String?
    /// A result's token estimate, from the compact number and the count.
    /// Built in: the context chips' "1.2K tokens".
    public var tokens: (@Sendable (String, Int) -> String)?
    /// Announced when results arrive. Built in: "1 result", "4 results".
    public var count: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - label: Names the list.
    ///   - group: The heading of a kind.
    ///   - loading: Read while searching.
    ///   - empty: Shown when nothing matches.
    ///   - error: Shown when the search failed.
    ///   - retry: The retry button.
    ///   - tokens: A result's token estimate.
    ///   - count: Announced when results arrive.
    public init(
        label: String? = nil,
        group: (@Sendable (MonarchAiMentionKind) -> String)? = nil,
        loading: String? = nil,
        empty: (@Sendable (String) -> String)? = nil,
        error: String? = nil,
        retry: String? = nil,
        tokens: (@Sendable (String, Int) -> String)? = nil,
        count: (@Sendable (Int) -> String)? = nil
    ) {
        self.label = label
        self.group = group
        self.loading = loading
        self.empty = empty
        self.error = error
        self.retry = retry
        self.tokens = tokens
        self.count = count
    }
}
