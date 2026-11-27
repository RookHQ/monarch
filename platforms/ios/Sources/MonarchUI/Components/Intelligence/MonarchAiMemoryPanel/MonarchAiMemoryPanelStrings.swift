/// Built-in strings of ``MonarchAiMemoryPanel``, from the `MonarchAiMemoryPanel` strings table.
enum MonarchAiMemoryPanelStrings {
    private static let table = "MonarchAiMemoryPanel"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiMemoryPanel." + key, table: table)
    }

    private static func counted(_ key: String, _ count: Int) -> String {
        let form = count == 1 ? ".one" : ".other"
        return MonarchStrings.string("monarch.aiMemoryPanel." + key + form, table: table, count)
    }

    /// The spoken name of the list.
    static var label: String { string("label") }
    /// The search prompt.
    static var search: String { string("search") }
    /// Starts selecting.
    static var select: String { string("select") }
    /// Stops selecting.
    static var done: String { string("done") }
    /// "Select all (4)".
    static func selectAll(_ shown: Int) -> String {
        MonarchStrings.string("monarch.aiMemoryPanel.selectAll", table: table, shown)
    }
    /// Clears a full selection.
    static var deselectAll: String { string("deselectAll") }
    /// The name of an entity kind.
    static func kind(_ kind: MonarchAiMemoryEntityKind) -> String { string("kind." + kind.rawValue) }
    /// "3 memories".
    static func count(_ count: Int) -> String { counted("count", count) }
    /// "Updated Mar 10, 2026".
    static func updated(_ date: String) -> String {
        MonarchStrings.string("monarch.aiMemoryPanel.updated", table: table, date)
    }
    /// Nothing is remembered.
    static var empty: String { string("empty") }
    /// The search matches nothing.
    static func noMatches(_ query: String) -> String {
        MonarchStrings.string("monarch.aiMemoryPanel.noMatches", table: table, query)
    }
    /// "2 memories match".
    static func matches(_ count: Int) -> String { counted("matches", count) }
    /// The forget command.
    static var forget: String { string("forget") }
    /// The disabled bulk command before anything is selected.
    static var forgetSelected: String { string("forgetSelected") }
    /// "Forget 2 memories?".
    static func confirmTitle(_ count: Int) -> String { counted("confirmTitle", count) }
    /// The confirmation text.
    static var confirmMessage: String { string("confirmMessage") }
    /// Dismisses the confirmation.
    static var cancel: String { string("cancel") }
    /// "2 memories forgotten".
    static func forgotten(_ count: Int) -> String { counted("forgotten", count) }
    /// Announced after an edit.
    static var edited: String { string("edited") }
}
