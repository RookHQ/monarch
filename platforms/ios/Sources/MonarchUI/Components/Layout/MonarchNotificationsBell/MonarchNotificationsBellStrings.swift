/// Built-in strings of the notifications bell, from the `MonarchNotificationsBell` strings table.
enum MonarchNotificationsBellStrings {
    private static let table = "MonarchNotificationsBell"

    /// The name of the button, and its visible title in the row variant.
    static var title: String { MonarchStrings.string("monarch.notificationsBell.title", table: table) }

    /// The spoken value for a number of unread notifications, already formatted.
    static func unread(_ count: String) -> String {
        MonarchStrings.string("monarch.notificationsBell.unread", table: table, count)
    }

    /// The badge text when the count is above the largest number shown.
    static func overflow(_ maximum: String) -> String {
        MonarchStrings.string("monarch.notificationsBell.overflow", table: table, maximum)
    }
}
