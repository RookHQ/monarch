/// Replacements for the built-in strings of ``MonarchCommentThread``.
/// A property that is `nil` keeps the built-in English string.
@_spi(Experimental) public struct MonarchCommentThreadLabels: Sendable {
    /// The name of the thread. Receives the first author. Built in: "Thread started by Ana Lopez".
    public var thread: (@Sendable (String) -> String)?
    /// The name of the quote. Built in: "Quoted text".
    public var quote: String?
    /// The button that shows folded replies. Receives the count. Built in: "Show 3 earlier replies".
    public var showEarlier: (@Sendable (Int) -> String)?
    /// Announced when folded replies are shown. Receives the count. Built in: "3 earlier replies shown".
    public var earlierShown: (@Sendable (Int) -> String)?
    /// The name of the reply field. Built in: "Reply".
    public var reply: String?
    /// The reply field's placeholder. Built in: "Reply…".
    public var replyPlaceholder: String?
    /// The send button. Built in: "Send reply".
    public var send: String?
    /// The resolve button. Built in: "Resolve".
    public var resolve: String?
    /// The reopen button. Built in: "Reopen".
    public var reopen: String?
    /// The badge and spoken state of a resolved thread. Built in: "Resolved".
    public var resolved: String?
    /// Shown instead of the reply field while resolved. Built in: "This thread is resolved. Reopen it to reply."
    public var resolvedHint: String?
    /// Announced after resolving. Built in: "Thread resolved".
    public var resolvedNotice: String?
    /// Announced after reopening. Built in: "Thread reopened".
    public var reopenedNotice: String?
    /// Announced after a reply is posted. Built in: "Reply sent".
    public var sentNotice: String?
    /// Shown and announced when an action fails. Built in: "That didn't work. Try again."
    public var actionFailed: String?
    /// The delete item of a comment's menu. Built in: "Delete comment".
    public var delete: String?
    /// The title of the delete confirmation. Built in: "Delete this comment?"
    public var deleteTitle: String?
    /// The message of the delete confirmation. Built in: "It is removed for everyone. This can't be undone."
    public var deleteMessage: String?
    /// The cancel button of the delete confirmation. Built in: "Cancel".
    public var cancel: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        thread: (@Sendable (String) -> String)? = nil,
        quote: String? = nil,
        showEarlier: (@Sendable (Int) -> String)? = nil,
        earlierShown: (@Sendable (Int) -> String)? = nil,
        reply: String? = nil,
        replyPlaceholder: String? = nil,
        send: String? = nil,
        resolve: String? = nil,
        reopen: String? = nil,
        resolved: String? = nil,
        resolvedHint: String? = nil,
        resolvedNotice: String? = nil,
        reopenedNotice: String? = nil,
        sentNotice: String? = nil,
        actionFailed: String? = nil,
        delete: String? = nil,
        deleteTitle: String? = nil,
        deleteMessage: String? = nil,
        cancel: String? = nil
    ) {
        self.thread = thread
        self.quote = quote
        self.showEarlier = showEarlier
        self.earlierShown = earlierShown
        self.reply = reply
        self.replyPlaceholder = replyPlaceholder
        self.send = send
        self.resolve = resolve
        self.reopen = reopen
        self.resolved = resolved
        self.resolvedHint = resolvedHint
        self.resolvedNotice = resolvedNotice
        self.reopenedNotice = reopenedNotice
        self.sentNotice = sentNotice
        self.actionFailed = actionFailed
        self.delete = delete
        self.deleteTitle = deleteTitle
        self.deleteMessage = deleteMessage
        self.cancel = cancel
    }
}
