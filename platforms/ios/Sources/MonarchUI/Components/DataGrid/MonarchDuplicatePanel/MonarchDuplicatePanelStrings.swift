/// Built-in strings of the duplicate panel, from the `MonarchDuplicatePanel`
/// strings table.
enum MonarchDuplicatePanelStrings {
    private static let table = "MonarchDuplicatePanel"

    /// The heading for a number of matches.
    static func title(count: Int) -> String {
        let key = count == 1 ? "monarch.duplicatePanel.title.one" : "monarch.duplicatePanel.title.other"
        return MonarchStrings.string(key, table: table)
    }

    /// The confidence of a match, for a formatted percentage.
    static func score(_ percentage: String) -> String {
        MonarchStrings.string("monarch.duplicatePanel.score", table: table, percentage)
    }

    /// The title of the button that opens a match.
    static var open: String { MonarchStrings.string("monarch.duplicatePanel.open", table: table) }
    /// The title of the button that merges a match.
    static var merge: String { MonarchStrings.string("monarch.duplicatePanel.merge", table: table) }
    /// The title of the button that says a match is not a duplicate.
    static var dismiss: String { MonarchStrings.string("monarch.duplicatePanel.dismiss", table: table) }

    /// The spoken name of an action on a named record.
    static func actionLabel(action: String, record: String) -> String {
        MonarchStrings.string("monarch.duplicatePanel.actionLabel", table: table, action, record)
    }
}
