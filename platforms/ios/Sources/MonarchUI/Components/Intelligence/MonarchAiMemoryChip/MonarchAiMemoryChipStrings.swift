/// Built-in strings of the AI memory chip, from the `MonarchAiMemoryChip` strings table.
enum MonarchAiMemoryChipStrings {
    private static let table = "MonarchAiMemoryChip"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiMemoryChip." + key, table: table)
    }

    /// The prefix for what happened to the memory.
    static func prefix(_ action: MonarchAiMemoryAction) -> String { string("prefix." + action.rawValue) }
    /// The spoken name of the options menu.
    static func options(_ text: String) -> String {
        MonarchStrings.string("monarch.aiMemoryChip.options", table: table, text)
    }
    /// The edit command.
    static var edit: String { string("edit") }
    /// The forget command.
    static var forget: String { string("forget") }
    /// The title of the edit dialog.
    static var editTitle: String { string("editTitle") }
    /// The name of the field in the edit dialog.
    static var editField: String { string("editField") }
    /// The save button.
    static var save: String { string("save") }
    /// The cancel button.
    static var cancel: String { string("cancel") }
}
