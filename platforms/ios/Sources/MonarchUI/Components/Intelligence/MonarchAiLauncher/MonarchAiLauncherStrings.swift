/// Built-in strings of the assistant launcher, from the `MonarchAiLauncher` strings table.
enum MonarchAiLauncherStrings {
    private static let table = "MonarchAiLauncher"

    /// The name of the button.
    static var label: String { MonarchStrings.string("monarch.aiLauncher.label", table: table) }
    /// Spoken as the value while the assistant is open.
    static var open: String { MonarchStrings.string("monarch.aiLauncher.open", table: table) }
    /// Spoken for the unread dot.
    static var unread: String { MonarchStrings.string("monarch.aiLauncher.unread", table: table) }
    /// Spoken for the working ring.
    static var working: String { MonarchStrings.string("monarch.aiLauncher.working", table: table) }

    /// Spoken for a count of unread messages.
    static func count(_ count: Int) -> String {
        count == 1
            ? MonarchStrings.string("monarch.aiLauncher.countOne", table: table)
            : MonarchStrings.string("monarch.aiLauncher.countOther", table: table, count)
    }

    /// The spoken text of a badge, or `nil` when the badge shows nothing.
    static func badge(_ badge: MonarchAiLauncherBadge?) -> String? {
        switch badge {
        case .none: nil
        case .unread: unread
        case .working: working
        case .count(let value): value > 0 ? count(value) : nil
        }
    }
}
