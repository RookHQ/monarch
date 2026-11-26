/// Built-in strings of the AI action log, from the `MonarchAiActionLog` strings table.
enum MonarchAiActionLogStrings {
    private static let table = "MonarchAiActionLog"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiActionLog.\(key)", table: table)
    }

    private static func string(_ key: String, _ argument: String) -> String {
        MonarchStrings.string("monarch.aiActionLog.\(key)", table: table, argument)
    }

    /// The spoken name of the list.
    static var label: String { string("label") }
    /// The undo button.
    static var undo: String { string("undo") }
    /// The spoken name of an undo button.
    static func undoLabel(_ entry: String) -> String { string("undoLabel", entry) }
    /// Shown on an undone entry.
    static var undone: String { string("undone") }
    /// Announced after an undo.
    static func undoneAnnouncement(_ entry: String) -> String { string("undoneAnnouncement", entry) }
    /// Shown and announced when an undo fails.
    static func undoFailed(_ entry: String) -> String { string("undoFailed", entry) }
    /// The accessibility action that opens a target.
    static func openTarget(_ target: String) -> String { string("openTarget", target) }
    /// Shown when there are no entries.
    static var empty: String { string("empty") }
    /// The show more button.
    static func showMore(_ count: Int) -> String {
        count == 1
            ? string("showMoreOne")
            : MonarchStrings.string("monarch.aiActionLog.showMoreOther", table: table, count)
    }
}
