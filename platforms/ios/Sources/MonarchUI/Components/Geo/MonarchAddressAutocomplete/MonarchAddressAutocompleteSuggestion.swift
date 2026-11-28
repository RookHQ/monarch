/// One suggestion from the provider of a ``MonarchAddressAutocomplete``.
@_spi(Experimental) public struct MonarchAddressAutocompleteSuggestion: Identifiable, Hashable, Sendable {
    /// A stable id from the provider, such as a place id.
    public let id: String
    /// The main line people read, such as "10 Market Street".
    public let label: String
    /// Secondary text, such as the city and the country.
    public let description: String?
    /// The structured address, when the search already returns it. Without
    /// it, `resolve` is called after the pick.
    public let address: MonarchAddressAutocompleteAddress?

    /// A suggestion.
    ///
    /// - Parameters:
    ///   - id: A stable id from the provider.
    ///   - label: The main line, already localised.
    ///   - description: Secondary text, such as the city and the country.
    ///   - address: The structured address, when the search returns it.
    public init(
        id: String,
        label: String,
        description: String? = nil,
        address: MonarchAddressAutocompleteAddress? = nil
    ) {
        self.id = id
        self.label = label
        self.description = description
        self.address = address
    }
}
