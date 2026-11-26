/// Built-in strings of the session list, from the `MonarchSessionList` strings table.
enum MonarchSessionListStrings {
    private static let table = "MonarchSessionList"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.sessionList.\(key)", table: table)
    }

    private static func string(_ key: String, _ argument: String) -> String {
        MonarchStrings.string("monarch.sessionList.\(key)", table: table, argument)
    }

    private static func string(_ key: String, count: Int) -> String {
        MonarchStrings.string("monarch.sessionList.\(key).\(count == 1 ? "one" : "other")", table: table, count)
    }

    /// The card title.
    static var title: String { string("title") }
    /// The line under the title.
    static var description: String { string("description") }
    /// The spoken name of the list.
    static var listLabel: String { string("listLabel") }
    /// The badge of the current session.
    static var current: String { string("current") }
    /// Before the sign-in time.
    static var signedIn: String { string("signedIn") }
    /// Before the last-active time.
    static var lastActive: String { string("lastActive") }
    /// The row button.
    static var signOut: String { string("signOut") }
    /// The spoken name of a row button.
    static func signOutLabel(_ label: String) -> String { string("signOutLabel", label) }
    /// The header button.
    static var signOutOthers: String { string("signOutOthers") }
    /// The confirmation title for one session.
    static func confirmTitle(_ label: String, isCurrent: Bool) -> String {
        isCurrent ? string("confirmTitleCurrent") : string("confirmTitle", label)
    }
    /// The confirmation message for one session.
    static func confirmMessage(isCurrent: Bool) -> String {
        string(isCurrent ? "confirmMessageCurrent" : "confirmMessage")
    }
    /// The confirmation title for every other session.
    static var othersTitle: String { string("othersTitle") }
    /// The confirmation message for every other session.
    static func othersMessage(_ count: Int) -> String { string("othersMessage", count: count) }
    /// The cancel button.
    static var cancel: String { string("cancel") }
    /// Announced after one session is signed out.
    static func signedOut(_ label: String) -> String { string("signedOut", label) }
    /// Announced after every other session is signed out.
    static func signedOutOthers(_ count: Int) -> String { string("signedOutOthers", count: count) }
    /// Shown when signing out one session fails.
    static func signOutFailed(_ label: String) -> String { string("signOutFailed", label) }
    /// Shown when signing out the other sessions fails.
    static var signOutOthersFailed: String { string("signOutOthersFailed") }
    /// Read while loading.
    static var loading: String { string("loading") }
    /// The empty state title.
    static var emptyTitle: String { string("emptyTitle") }
    /// The error state title.
    static var errorTitle: String { string("errorTitle") }
}
