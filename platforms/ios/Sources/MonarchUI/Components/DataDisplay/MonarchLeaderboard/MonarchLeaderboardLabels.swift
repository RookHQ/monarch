/// Replacements for the built-in strings of ``MonarchLeaderboard``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchLeaderboardLabels: Sendable {
    /// The tag after the viewer's own name. Built in: "(you)".
    public var you: String?
    /// Shown when there are no entries. Built in: "Nothing to rank yet".
    public var empty: String?
    /// The spoken name of the loading placeholder. Built in: "Loading ranking".
    public var loading: String?
    /// The title of the retry button after a failure. Built in: the retry title of ``MonarchInlineErrorRetry``.
    public var retry: String?
    /// The text for entries not shown, from how many. Built in: "4 more".
    public var more: (@Sendable (Int) -> String)?
    /// The spoken rank before the name. Built in: "Rank 1:".
    public var rank: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        you: String? = nil,
        empty: String? = nil,
        loading: String? = nil,
        retry: String? = nil,
        more: (@Sendable (Int) -> String)? = nil,
        rank: (@Sendable (Int) -> String)? = nil
    ) {
        self.you = you
        self.empty = empty
        self.loading = loading
        self.retry = retry
        self.more = more
        self.rank = rank
    }
}
