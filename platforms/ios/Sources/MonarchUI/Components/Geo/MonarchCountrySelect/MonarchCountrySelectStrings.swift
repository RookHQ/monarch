/// Built-in strings of ``MonarchCountrySelect``, from the `MonarchCountrySelect` strings table.
enum MonarchCountrySelectStrings {
    private static let table = "MonarchCountrySelect"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.countrySelect.\(key)", table: table)
    }

    /// The heading over the suggested countries.
    static var suggested: String { string("suggested") }
    /// The heading over the other countries.
    static var all: String { string("all") }
    /// Shown when no country matches the search.
    static var empty: String { string("empty") }
}
