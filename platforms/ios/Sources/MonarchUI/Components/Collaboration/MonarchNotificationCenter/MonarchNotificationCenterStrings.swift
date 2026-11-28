/// Built-in strings of the notification center, from the `MonarchNotificationCenter` strings table.
enum MonarchNotificationCenterStrings {
    private static let table = "MonarchNotificationCenter"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.notificationCenter.\(key)", table: table)
    }

    /// The navigation title.
    static var title: String { string("title") }
    /// The All tab.
    static var all: String { string("all") }
    /// The Unread tab.
    static var unread: String { string("unread") }
    /// The name of the tabs.
    static var filter: String { string("filter") }
    /// The mark-all button.
    static var markAllRead: String { string("markAllRead") }
    /// The swipe action on an unread item.
    static var markRead: String { string("markRead") }
    /// Read after the title of an unread item.
    static var unreadItem: String { string("unreadItem") }
    /// The heading of today's items.
    static var today: String { string("today") }
    /// The heading of yesterday's items.
    static var yesterday: String { string("yesterday") }
    /// The empty state.
    static var emptyTitle: String { string("emptyTitle") }
    /// The empty state's text.
    static var emptyMessage: String { string("emptyMessage") }
    /// The empty state of the Unread tab.
    static var emptyUnreadTitle: String { string("emptyUnreadTitle") }
    /// The loading placeholder.
    static var loading: String { string("loading") }
    /// The button that closes the panel.
    static var done: String { string("done") }
}
