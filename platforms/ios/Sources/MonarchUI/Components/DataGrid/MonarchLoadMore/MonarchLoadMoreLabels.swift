/// Replacements for the built-in strings of ``MonarchLoadMore``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchLoadMoreLabels: Sendable {
    /// The button title. Built in: "Load more".
    public var loadMore: String?
    /// The button title while a page loads. Built in: "Loading…".
    public var loading: String?
    /// Shown and announced when a page fails. Built in: "Couldn't load more."
    public var failed: String?
    /// The retry button title. Built in: "Retry".
    public var retry: String?
    /// Shown and announced when everything is loaded. Built in: "That's everything".
    public var end: String?
    /// The count. Receives the loaded count and the total, if known. Built
    /// in: "Showing 40 of 120 items", or "Showing 40 items".
    public var count: (@Sendable (Int, Int?) -> String)?
    /// Announced when a page arrives. Receives the number added. Built in:
    /// "20 more items loaded."
    public var loaded: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        loadMore: String? = nil,
        loading: String? = nil,
        failed: String? = nil,
        retry: String? = nil,
        end: String? = nil,
        count: (@Sendable (Int, Int?) -> String)? = nil,
        loaded: (@Sendable (Int) -> String)? = nil
    ) {
        self.loadMore = loadMore
        self.loading = loading
        self.failed = failed
        self.retry = retry
        self.end = end
        self.count = count
        self.loaded = loaded
    }
}
