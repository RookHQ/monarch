/// Built-in strings of the security alert card, from the `MonarchSecurityAlertCard` strings table.
enum MonarchSecurityAlertCardStrings {
    private static let table = "MonarchSecurityAlertCard"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.securityAlertCard.\(key)", table: table)
    }

    /// The card title.
    static var title: String { string("title") }
    /// The line under the title.
    static var description: String { string("description") }
    /// The spoken name of the list.
    static var listLabel: String { string("listLabel") }
    /// Read for an unread alert.
    static var unread: String { string("unread") }
    /// The row action.
    static var markRead: String { string("markRead") }
    /// The header action.
    static var markAllRead: String { string("markAllRead") }
    /// Shown while loading.
    static var loading: String { string("loading") }
    /// The empty state title.
    static var emptyTitle: String { string("emptyTitle") }
    /// The empty state message.
    static var emptyDescription: String { string("emptyDescription") }
    /// The error state title.
    static var errorTitle: String { string("errorTitle") }

    /// The name of a severity.
    static func severity(_ severity: MonarchSecurityAlertSeverity) -> String {
        string("severity.\(severity.rawValue)")
    }

    /// The unread count.
    static func unreadCount(_ count: Int) -> String {
        MonarchStrings.string("monarch.securityAlertCard.unreadCount", table: table, count)
    }

    /// The spoken name of a dismiss button.
    static func dismiss(_ title: String) -> String {
        MonarchStrings.string("monarch.securityAlertCard.dismiss", table: table, title)
    }

    /// Announced after an alert is dismissed.
    static func dismissed(_ title: String) -> String {
        MonarchStrings.string("monarch.securityAlertCard.dismissed", table: table, title)
    }
}
