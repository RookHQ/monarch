/// Built-in strings of ``MonarchRegionSelect``, from the `MonarchRegionSelect` strings table.
enum MonarchRegionSelectStrings {
    private static let table = "MonarchRegionSelect"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.regionSelect.\(key)", table: table)
    }

    /// The prompt while no country is chosen.
    static var chooseCountryFirst: String { string("chooseCountryFirst") }
    /// The prompt of a picker with a list while nothing is chosen.
    static var choose: String { string("choose") }
    /// The prompt of the text field for a country without a list.
    static var textEntry: String { string("textEntry") }
    /// Shown when no region matches the search.
    static var empty: String { string("empty") }
    /// Shown when the regions could not be loaded.
    static var loadFailed: String { string("loadFailed") }

    /// The country's word for its regions, such as "State", or `nil` when
    /// Monarch has no list for the country.
    static func fieldLabel(_ country: String) -> String? {
        let key = "monarch.regionSelect.label.\(country)"
        let value = MonarchStrings.string(key, table: table)
        return value == key ? nil : value
    }
}
