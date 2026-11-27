/// Built-in strings of ``MonarchDependencyPicker``, from the `MonarchDependencyPicker` strings table.
enum MonarchDependencyPickerStrings {
    private static let table = "MonarchDependencyPicker"

    /// The string for a key without placeholders, such as `empty`.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.dependencyPicker.\(key)", table: table)
    }

    /// The string for a key with one text placeholder.
    static func string(_ key: String, _ text: String) -> String {
        MonarchStrings.string("monarch.dependencyPicker.\(key)", table: table, text)
    }

    /// The string for a key with two text placeholders: the relation and the item.
    static func string(_ key: String, _ relation: String, _ item: String) -> String {
        MonarchStrings.string("monarch.dependencyPicker.\(key)", table: table, relation, item)
    }
}
