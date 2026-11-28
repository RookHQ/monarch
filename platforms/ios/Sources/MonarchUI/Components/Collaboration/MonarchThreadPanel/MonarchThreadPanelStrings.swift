/// Built-in strings of the thread panel, from the `MonarchThreadPanel` strings table.
enum MonarchThreadPanelStrings {
    private static let table = "MonarchThreadPanel"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.threadPanel.\(key)", table: table)
    }

    /// The navigation title.
    static var title: String { string("title") }
    /// The close button.
    static var close: String { string("close") }
    /// The follow button while not following.
    static var follow: String { string("follow") }
    /// The follow button while following.
    static var following: String { string("following") }
    /// Announced after following.
    static var followedNotice: String { string("followedNotice") }
    /// Announced after unfollowing.
    static var unfollowedNotice: String { string("unfollowedNotice") }
    /// Announced when the change fails.
    static var followFailed: String { string("followFailed") }
    /// The name of the list of replies.
    static var replies: String { string("replies") }
    /// Spoken while loading.
    static var loading: String { string("loading") }
    /// Shown when loading failed.
    static var error: String { string("error") }
    /// Shown for a deleted thread.
    static var deleted: String { string("deleted") }

    /// "3 replies" (English plural rule).
    static func replyCount(_ count: Int) -> String {
        let key = count == 1 ? "monarch.threadPanel.replyCount.one" : "monarch.threadPanel.replyCount.other"
        return MonarchStrings.string(key, table: table, count)
    }
}
