/// Built-in strings of ``MonarchCurrencySelect``, from the `MonarchCurrencySelect` strings table.
enum MonarchCurrencySelectStrings {
    private static let table = "MonarchCurrencySelect"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.currencySelect.\(key)", table: table)
    }

    /// The heading over the suggested currencies.
    static var suggested: String { string("suggested") }
    /// The heading over the other currencies.
    static var all: String { string("all") }
    /// Shown when no currency matches the search.
    static var empty: String { string("empty") }
}
