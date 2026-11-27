/// Built-in strings of ``MonarchActionEditor``, from the `MonarchActionEditor` strings table.
enum MonarchActionEditorStrings {
    private static let table = "MonarchActionEditor"

    /// The string for a key without placeholders, such as `add`.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.actionEditor.\(key)", table: table)
    }

    /// The string for a key with a number placeholder, such as "Action 1".
    static func string(_ key: String, _ number: Int) -> String {
        MonarchStrings.string("monarch.actionEditor.\(key)", table: table, number)
    }

    /// The string for a key with a text placeholder, such as "Fill in “Team”.".
    static func string(_ key: String, _ text: String) -> String {
        MonarchStrings.string("monarch.actionEditor.\(key)", table: table, text)
    }
}
