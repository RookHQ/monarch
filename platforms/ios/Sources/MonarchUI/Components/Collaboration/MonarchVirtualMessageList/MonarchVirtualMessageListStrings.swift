/// Built-in strings of the message list, from the `MonarchVirtualMessageList` strings table.
enum MonarchVirtualMessageListStrings {
    private static let table = "MonarchVirtualMessageList"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.virtualMessageList.\(key)", table: table)
    }

    /// The name of the list.
    static var list: String { string("list") }
    /// The unread divider.
    static var newMessages: String { string("newMessages") }
    /// The separator for today.
    static var today: String { string("today") }
    /// The separator for yesterday.
    static var yesterday: String { string("yesterday") }
    /// The jump button.
    static var jumpToLatest: String { string("jumpToLatest") }
    /// The load-older button.
    static var loadOlder: String { string("loadOlder") }
    /// The load-older button while loading.
    static var loadingOlder: String { string("loadingOlder") }
    /// The load-older failure.
    static var olderFailed: String { string("olderFailed") }
    /// Spoken while the first messages load.
    static var loading: String { string("loading") }

    /// "3 new messages" (English plural rule).
    static func newCount(_ count: Int) -> String {
        let key =
            count == 1 ? "monarch.virtualMessageList.newCount.one" : "monarch.virtualMessageList.newCount.other"
        return MonarchStrings.string(key, table: table, count)
    }
}
