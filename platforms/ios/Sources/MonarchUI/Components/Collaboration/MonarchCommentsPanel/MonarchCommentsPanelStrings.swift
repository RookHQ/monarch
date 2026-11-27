/// Built-in strings of the comments panel, from the `MonarchCommentsPanel` strings table.
enum MonarchCommentsPanelStrings {
    private static let table = "MonarchCommentsPanel"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.commentsPanel.\(key)", table: table)
    }

    /// The navigation title.
    static var title: String { string("title") }
    /// The close button.
    static var close: String { string("close") }
    /// The name of the filter picker.
    static var filter: String { string("filter") }
    /// The Open segment.
    static var open: String { string("open") }
    /// The Resolved segment.
    static var resolved: String { string("resolved") }
    /// Spoken while loading.
    static var loading: String { string("loading") }
    /// The empty title under Open.
    static var emptyOpen: String { string("emptyOpen") }
    /// The empty title under Resolved.
    static var emptyResolved: String { string("emptyResolved") }
    /// The hint under the empty title for Open.
    static var emptyHint: String { string("emptyHint") }

    /// "3 comments" (English plural rule).
    static func commentCount(_ count: Int) -> String {
        let key = count == 1 ? "monarch.commentsPanel.commentCount.one" : "monarch.commentsPanel.commentCount.other"
        return MonarchStrings.string(key, table: table, count)
    }
}
