/// Built-in strings of the slash commands, from the `MonarchAiSlashCommands` strings table.
enum MonarchAiSlashCommandsStrings {
    private static let table = "MonarchAiSlashCommands"

    /// The spoken name of the list.
    static var label: String { MonarchStrings.string("monarch.aiSlashCommands.label", table: table) }

    /// Shown when nothing matches what was typed.
    static func empty(_ typed: String) -> String {
        MonarchStrings.string("monarch.aiSlashCommands.empty", table: table, typed)
    }

    /// Announced when the list changes.
    static func count(_ count: Int) -> String {
        count == 1
            ? MonarchStrings.string("monarch.aiSlashCommands.countOne", table: table)
            : MonarchStrings.string("monarch.aiSlashCommands.countOther", table: table, count)
    }
}
