/// Built-in strings of ``MonarchAddressAutocomplete``, from the `MonarchAddressAutocomplete` strings table.
enum MonarchAddressAutocompleteStrings {
    private static let table = "MonarchAddressAutocomplete"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.addressAutocomplete.\(key)", table: table)
    }

    /// The label of the address search field.
    static var label: String { string("label") }
    /// Shown in the address search field while it is empty.
    static var placeholder: String { string("placeholder") }
    /// Shown in the address suggestion list while suggestions load.
    static var searching: String { string("searching") }
    /// Shown while the details of the picked address load.
    static var resolving: String { string("resolving") }
    /// Shown when the address search finds nothing.
    static var empty: String { string("empty") }
    /// Shown when the address search fails. A retry button follows.
    static var searchFailed: String { string("searchFailed") }
    /// Shown when the details of the picked address could not be loaded.
    static var resolveFailed: String { string("resolveFailed") }
    /// The button and list row that open the address form.
    static var enterManually: String { string("enterManually") }
    /// The button that goes back from the address form to the search field.
    static var backToSearch: String { string("backToSearch") }
    /// Announced after a suggestion fills in the address form.
    static var filled: String { string("filled") }
    /// The button that closes the address suggestion list.
    static var cancel: String { string("cancel") }
    /// The label of the first street line.
    static var line1: String { string("line1") }
    /// The label of the second street line.
    static var line2: String { string("line2") }
    /// The label of the city field.
    static var city: String { string("city") }
    /// The label of the state, province or region field.
    static var region: String { string("region") }
    /// The label of the postal code field.
    static var postalCode: String { string("postalCode") }
    /// The label of the country field.
    static var country: String { string("country") }
    /// Shown under the labels of optional address fields.
    static var optional: String { string("optional") }
    /// The spoken value of the address search field when no address is filled in.
    static var noAddress: String { string("noAddress") }
    /// "Type at least 3 characters to search."
    static func tooShort(_ length: Int) -> String {
        MonarchStrings.string("monarch.addressAutocomplete.tooShort", table: table, length)
    }
    /// "1 address found" or "3 addresses found".
    static func results(_ count: Int) -> String {
        count == 1
            ? string("resultsOne")
            : MonarchStrings.string("monarch.addressAutocomplete.resultsMany", table: table, count)
    }
}
