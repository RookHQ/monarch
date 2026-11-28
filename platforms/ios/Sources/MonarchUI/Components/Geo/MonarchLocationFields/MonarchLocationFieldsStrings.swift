/// Built-in strings of ``MonarchLocationFields``, from the `MonarchLocationFields` strings table.
enum MonarchLocationFieldsStrings {
    private static let table = "MonarchLocationFields"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.locationFields.\(key)", table: table)
    }

    /// The country field's label.
    static var country: String { string("country") }
    /// The region field's label when the country's word is unknown.
    static var region: String { string("region") }
    /// The city field's label.
    static var city: String { string("city") }
    /// The postal code field's label when the country's word is unknown.
    static var postalCode: String { string("postalCode") }
    /// The countries could not be loaded.
    static var countriesLoadFailed: String { string("countriesLoadFailed") }
    /// The city search failed.
    static var citiesLoadFailed: String { string("citiesLoadFailed") }
    /// The prompt while no country is chosen.
    static var chooseCountryFirst: String { string("chooseCountryFirst") }
    /// No city matches.
    static var noMatch: String { string("noMatch") }
    /// "Type at least 2 letters to search."
    static func typeMore(_ count: Int) -> String {
        MonarchStrings.string("monarch.locationFields.typeMore", table: table, count)
    }
    /// "Use “Paris”".
    static func useTyped(_ text: String) -> String {
        MonarchStrings.string("monarch.locationFields.useTyped", table: table, text)
    }

    /// The country's word for its postal code, such as "ZIP code", or `nil`
    /// when Monarch has none for the country.
    static func postalCodeLabel(_ country: String) -> String? {
        let key = "monarch.locationFields.postalCode.\(country)"
        let value = MonarchStrings.string(key, table: table)
        return value == key ? nil : value
    }
}
