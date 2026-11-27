/// Built-in strings of ``MonarchQueryBuilder``, from the `MonarchQueryBuilder` strings table.
enum MonarchQueryBuilderStrings {
    private static let table = "MonarchQueryBuilder"

    /// The string for a key without placeholders, such as `addRule`.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.queryBuilder.\(key)", table: table)
    }

    /// The string for a key with a text placeholder, such as "Remove group 2".
    static func string(_ key: String, _ text: String) -> String {
        MonarchStrings.string("monarch.queryBuilder.\(key)", table: table, text)
    }

    /// The string for a key with a text and a number placeholder, such as "Top group, rule 1".
    static func string(_ key: String, _ text: String, _ number: Int) -> String {
        MonarchStrings.string("monarch.queryBuilder.\(key)", table: table, text, number)
    }

    /// The string for a key with a number placeholder, such as "Rule 1".
    static func string(_ key: String, _ number: Int) -> String {
        MonarchStrings.string("monarch.queryBuilder.\(key)", table: table, number)
    }
}
