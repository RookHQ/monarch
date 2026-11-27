/// Replacements for the built-in strings of ``MonarchCursorList``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchCursorListLabels: Sendable {
    /// The spoken name of the first-page placeholder. Built in: "Loading".
    public var loading: String?
    /// The title of the empty state. Built in: "Nothing here yet".
    public var emptyTitle: String?
    /// The message of the empty state. Built in: "Items appear here once they are added."
    public var emptyMessage: String?
    /// The title of the error state when the first page fails. Built in: "This list could not be loaded".
    public var errorTitle: String?
    /// The retry button title, in the error state and the footer. Built in: "Retry".
    public var retry: String?
    /// Replacements for the strings of the ``MonarchLoadMore`` footer: the
    /// button, the count, the end line and the announcements.
    public var footer: MonarchLoadMoreLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        loading: String? = nil,
        emptyTitle: String? = nil,
        emptyMessage: String? = nil,
        errorTitle: String? = nil,
        retry: String? = nil,
        footer: MonarchLoadMoreLabels = MonarchLoadMoreLabels()
    ) {
        self.loading = loading
        self.emptyTitle = emptyTitle
        self.emptyMessage = emptyMessage
        self.errorTitle = errorTitle
        self.retry = retry
        self.footer = footer
    }
}
