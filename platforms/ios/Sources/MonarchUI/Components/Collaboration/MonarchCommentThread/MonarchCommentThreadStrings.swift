/// Built-in strings of the comment thread, from the `MonarchCommentThread` strings table.
enum MonarchCommentThreadStrings {
    private static let table = "MonarchCommentThread"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.commentThread.\(key)", table: table)
    }

    /// "Thread started by Ana Lopez".
    static func thread(_ author: String) -> String {
        MonarchStrings.string("monarch.commentThread.thread", table: table, author)
    }

    /// "Show 3 earlier replies" (English plural rule).
    static func showEarlier(_ count: Int) -> String {
        let key = count == 1 ? "monarch.commentThread.showEarlier.one" : "monarch.commentThread.showEarlier.other"
        return MonarchStrings.string(key, table: table, count)
    }

    /// "3 earlier replies shown" (English plural rule).
    static func earlierShown(_ count: Int) -> String {
        let key = count == 1 ? "monarch.commentThread.earlierShown.one" : "monarch.commentThread.earlierShown.other"
        return MonarchStrings.string(key, table: table, count)
    }

    /// The name of the quote.
    static var quote: String { string("quote") }
    /// The name of the reply field.
    static var reply: String { string("reply") }
    /// The reply field's placeholder.
    static var replyPlaceholder: String { string("replyPlaceholder") }
    /// The send button.
    static var send: String { string("send") }
    /// The resolve button.
    static var resolve: String { string("resolve") }
    /// The reopen button.
    static var reopen: String { string("reopen") }
    /// The resolved badge and state.
    static var resolved: String { string("resolved") }
    /// The hint shown while resolved.
    static var resolvedHint: String { string("resolvedHint") }
    /// Announced after resolving.
    static var resolvedNotice: String { string("resolvedNotice") }
    /// Announced after reopening.
    static var reopenedNotice: String { string("reopenedNotice") }
    /// Announced after a reply is posted.
    static var sentNotice: String { string("sentNotice") }
    /// Shown when an action fails.
    static var actionFailed: String { string("actionFailed") }
    /// The delete menu item.
    static var delete: String { string("delete") }
    /// The delete confirmation's title.
    static var deleteTitle: String { string("deleteTitle") }
    /// The delete confirmation's message.
    static var deleteMessage: String { string("deleteMessage") }
    /// The delete confirmation's cancel button.
    static var cancel: String { string("cancel") }
}
