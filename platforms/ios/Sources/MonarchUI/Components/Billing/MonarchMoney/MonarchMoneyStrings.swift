/// Built-in strings of ``MonarchMoney``, from the `MonarchMoney` strings table.
enum MonarchMoneyStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.money." + key, table: "MonarchMoney")
    }

    /// Shown in place of a missing amount.
    static var empty: String { string("empty") }
    /// Read in place of a missing amount.
    static var emptyLabel: String { string("emptyLabel") }
}
