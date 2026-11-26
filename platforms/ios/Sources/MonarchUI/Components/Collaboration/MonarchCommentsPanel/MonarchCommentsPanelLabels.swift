/// Replacements for the built-in strings of ``MonarchCommentsPanel``.
/// A property that is `nil` keeps the built-in English string.
@_spi(Experimental) public struct MonarchCommentsPanelLabels: Sendable {
    /// The navigation title. Built in: "Comments".
    public var title: String?
    /// The close button. Built in: "Close".
    public var close: String?
    /// The name of the Open and Resolved picker. Built in: "Show".
    public var filter: String?
    /// The Open segment. Built in: "Open".
    public var open: String?
    /// The Resolved segment. Built in: "Resolved".
    public var resolved: String?
    /// Spoken while the threads load. Built in: "Loading comments".
    public var loading: String?
    /// The empty title under Open. Built in: "No open comments".
    public var emptyOpen: String?
    /// The empty title under Resolved. Built in: "No resolved comments".
    public var emptyResolved: String?
    /// The hint under the empty title for Open. Built in: "Select text to start a discussion."
    public var emptyHint: String?
    /// The number of comments of a thread, in the list and as the thread's title. Built in: "3 comments".
    public var commentCount: (@Sendable (Int) -> String)?
    /// The strings of the open thread, a ``MonarchCommentThread``.
    public var thread: MonarchCommentThreadLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        close: String? = nil,
        filter: String? = nil,
        open: String? = nil,
        resolved: String? = nil,
        loading: String? = nil,
        emptyOpen: String? = nil,
        emptyResolved: String? = nil,
        emptyHint: String? = nil,
        commentCount: (@Sendable (Int) -> String)? = nil,
        thread: MonarchCommentThreadLabels = MonarchCommentThreadLabels()
    ) {
        self.title = title
        self.close = close
        self.filter = filter
        self.open = open
        self.resolved = resolved
        self.loading = loading
        self.emptyOpen = emptyOpen
        self.emptyResolved = emptyResolved
        self.emptyHint = emptyHint
        self.commentCount = commentCount
        self.thread = thread
    }
}
