/// Built-in strings of ``MonarchConditionEditor``, from the `MonarchConditionEditor` strings table.
///
/// The operator names, problems, "Choose…", "Choose a date" and the removed-field text are
/// shared with ``MonarchQueryBuilder`` and read from its table, so both say the same thing.
enum MonarchConditionEditorStrings {
    private static let table = "MonarchConditionEditor"

    /// The string for a key without placeholders, such as `add`.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.conditionEditor.\(key)", table: table)
    }

    /// The string for a key with a number placeholder, such as "Condition 1".
    static func string(_ key: String, _ number: Int) -> String {
        MonarchStrings.string("monarch.conditionEditor.\(key)", table: table, number)
    }
}
