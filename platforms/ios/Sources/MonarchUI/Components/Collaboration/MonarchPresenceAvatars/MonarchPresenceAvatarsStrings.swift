/// Built-in strings of the presence avatars, from the `MonarchPresenceAvatars` strings table.
enum MonarchPresenceAvatarsStrings {
    private static let table = "MonarchPresenceAvatars"

    /// The spoken name of the stack.
    static func label(_ count: Int) -> String {
        count == 1
            ? MonarchStrings.string("monarch.presenceAvatars.labelOne", table: table)
            : MonarchStrings.string("monarch.presenceAvatars.labelOther", table: table, count)
    }

    /// The hint of a stack that opens the list.
    static var showAll: String { MonarchStrings.string("monarch.presenceAvatars.showAll", table: table) }

    /// The title of the list.
    static var list: String { MonarchStrings.string("monarch.presenceAvatars.list", table: table) }

    /// The button that closes the list.
    static var done: String { MonarchStrings.string("monarch.presenceAvatars.done", table: table) }

    /// The spoken status of a person with an activity.
    static func status(_ presence: String, _ activity: String) -> String {
        MonarchStrings.string("monarch.presenceAvatars.status", table: table, presence, activity)
    }

    /// The second line of a person in the list, with an activity.
    static func detail(_ presence: String, _ activity: String) -> String {
        MonarchStrings.string("monarch.presenceAvatars.detail", table: table, presence, activity)
    }
}
