/// Replacements for the built-in strings of ``MonarchAddressAutocomplete``.
/// A property that is `nil` keeps the built-in English string.
@_spi(Experimental) public struct MonarchAddressAutocompleteLabels: Sendable {
    /// Shown in the address search field while it is empty. Built in: "Start typing an address".
    public var placeholder: String?
    /// Shown in the address suggestion list while suggestions load. Built in: "Searching…".
    public var searching: String?
    /// Shown while the details of the picked address load. Built in: "Getting the address details…".
    public var resolving: String?
    /// Shown when the address search finds nothing. Built in: "No addresses found".
    public var empty: String?
    /// Shown when the address search fails. A retry button follows. Built in: "Couldn’t search addresses."
    public var searchFailed: String?
    /// Shown when the details of the picked address could not be loaded. Built in: "Couldn’t get that address. Enter it manually or try another."
    public var resolveFailed: String?
    /// The button and list row that open the address form. Built in: "Enter the address manually".
    public var enterManually: String?
    /// The button that goes back from the address form to the search field. Built in: "Search for an address".
    public var backToSearch: String?
    /// Announced after a suggestion fills in the address form. Built in: "Address filled in. Check the details."
    public var filled: String?
    /// The button that closes the address suggestion list. Built in: "Cancel".
    public var cancel: String?
    /// The label of the first street line. Built in: "Street address".
    public var line1: String?
    /// The label of the second street line. Built in: "Apartment, suite, etc."
    public var line2: String?
    /// The label of the city field. Built in: "City".
    public var city: String?
    /// The label of the state, province or region field. Built in: "State or region".
    public var region: String?
    /// The label of the postal code field. Built in: "Postal code".
    public var postalCode: String?
    /// The label of the country field. Built in: "Country".
    public var country: String?
    /// Shown under the labels of optional address fields. Built in: "Optional".
    public var optional: String?
    /// The spoken value of the address search field when no address is filled in. Built in: "None".
    public var noAddress: String?
    /// Shown until the search is long enough. Receives the length. Built in: "Type at least 3 characters to search."
    public var tooShort: (@Sendable (Int) -> String)?
    /// Announced with the number of suggestions. Built in: "1 address found", "3 addresses found".
    public var results: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        placeholder: String? = nil,
        searching: String? = nil,
        resolving: String? = nil,
        empty: String? = nil,
        searchFailed: String? = nil,
        resolveFailed: String? = nil,
        enterManually: String? = nil,
        backToSearch: String? = nil,
        filled: String? = nil,
        cancel: String? = nil,
        line1: String? = nil,
        line2: String? = nil,
        city: String? = nil,
        region: String? = nil,
        postalCode: String? = nil,
        country: String? = nil,
        optional: String? = nil,
        noAddress: String? = nil,
        tooShort: (@Sendable (Int) -> String)? = nil,
        results: (@Sendable (Int) -> String)? = nil
    ) {
        self.placeholder = placeholder
        self.searching = searching
        self.resolving = resolving
        self.empty = empty
        self.searchFailed = searchFailed
        self.resolveFailed = resolveFailed
        self.enterManually = enterManually
        self.backToSearch = backToSearch
        self.filled = filled
        self.cancel = cancel
        self.line1 = line1
        self.line2 = line2
        self.city = city
        self.region = region
        self.postalCode = postalCode
        self.country = country
        self.optional = optional
        self.noAddress = noAddress
        self.tooShort = tooShort
        self.results = results
    }
}
