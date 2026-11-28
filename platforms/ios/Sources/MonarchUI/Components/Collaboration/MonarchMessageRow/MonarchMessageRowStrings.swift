/// Built-in strings of the message row, from the `MonarchMessageRow` strings table.
enum MonarchMessageRowStrings {
    private static let table = "MonarchMessageRow"

    /// Shown while a message is being sent.
    static var sending: String { MonarchStrings.string("monarch.messageRow.sending", table: table) }

    /// Shown when a message failed without an error message.
    static var failed: String { MonarchStrings.string("monarch.messageRow.failed", table: table) }

    /// The retry button.
    static var retry: String { MonarchStrings.string("monarch.messageRow.retry", table: table) }

    /// The discard button.
    static var discard: String { MonarchStrings.string("monarch.messageRow.discard", table: table) }

    /// The edited marker.
    static var edited: String { MonarchStrings.string("monarch.messageRow.edited", table: table) }

    /// The VoiceOver action that opens the edit history.
    static var editHistory: String { MonarchStrings.string("monarch.messageRow.editHistory", table: table) }

    /// The body of a deleted message.
    static var deleted: String { MonarchStrings.string("monarch.messageRow.deleted", table: table) }

    /// The thread button.
    static func replies(_ count: Int) -> String {
        count == 1
            ? MonarchStrings.string("monarch.messageRow.reply", table: table)
            : MonarchStrings.string("monarch.messageRow.replies", table: table, count)
    }

    /// The author and the time, read by VoiceOver for a grouped message.
    static func byline(_ author: String, _ time: String) -> String {
        MonarchStrings.string("monarch.messageRow.byline", table: table, author, time)
    }
}
