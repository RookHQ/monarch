/// Built-in strings of ``MonarchPhoneCountrySelect``, from the `MonarchPhoneCountrySelect` strings table.
enum MonarchPhoneCountrySelectStrings {
    private static let table = "MonarchPhoneCountrySelect"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.phoneCountrySelect.\(key)", table: table)
    }

    /// The heading over the preferred countries.
    static var suggested: String { string("suggested") }
    /// The heading over the other countries.
    static var all: String { string("all") }
    /// Shown when no country matches the search.
    static var empty: String { string("empty") }
    /// A row's text: the country name and the calling code.
    static func option(name: String, dialCode: String) -> String {
        MonarchStrings.string("monarch.phoneCountrySelect.option", table: table, name, dialCode)
    }
}
