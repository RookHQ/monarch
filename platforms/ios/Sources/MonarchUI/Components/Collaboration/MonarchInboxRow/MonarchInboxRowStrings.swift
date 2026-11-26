/// Built-in strings of the inbox row, from the `MonarchInboxRow` strings table.
enum MonarchInboxRowStrings {
    private static let table = "MonarchInboxRow"

    /// The spoken unread state.
    static func unread(_ count: Int?) -> String {
        guard let count else { return MonarchStrings.string("monarch.inboxRow.unread", table: table) }
        return MonarchStrings.string("monarch.inboxRow.unreadCount", table: table, count)
    }

    /// The title followed by the unread state, for VoiceOver.
    static func titleWithState(_ title: String, _ state: String) -> String {
        MonarchStrings.string("monarch.inboxRow.titleWithState", table: table, title, state)
    }

    /// The compact age.
    static func age(_ unit: MonarchInboxRowAgeUnit, _ value: Int) -> String {
        switch unit {
        case .now: MonarchStrings.string("monarch.inboxRow.ageNow", table: table)
        case .minutes: MonarchStrings.string("monarch.inboxRow.ageMinutes", table: table, value)
        case .hours: MonarchStrings.string("monarch.inboxRow.ageHours", table: table, value)
        case .days: MonarchStrings.string("monarch.inboxRow.ageDays", table: table, value)
        case .weeks: MonarchStrings.string("monarch.inboxRow.ageWeeks", table: table, value)
        }
    }

    /// The mark-as-read swipe action.
    static var markRead: String { MonarchStrings.string("monarch.inboxRow.markRead", table: table) }

    /// The mark-as-unread swipe action.
    static var markUnread: String { MonarchStrings.string("monarch.inboxRow.markUnread", table: table) }

    /// The archive swipe action.
    static var archive: String { MonarchStrings.string("monarch.inboxRow.archive", table: table) }

    /// The delete swipe action.
    static var delete: String { MonarchStrings.string("monarch.inboxRow.delete", table: table) }
}
