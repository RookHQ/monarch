/// Built-in strings of tabular numbers, from the
/// `MonarchTabularNumberStyle` strings table.
enum MonarchTabularNumberStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.tabularNumberStyle." + key, table: "MonarchTabularNumberStyle")
    }

    /// Shown in place of a missing number.
    static var empty: String { string("empty") }
    /// Read in place of a missing number.
    static var emptyLabel: String { string("emptyLabel") }
}
