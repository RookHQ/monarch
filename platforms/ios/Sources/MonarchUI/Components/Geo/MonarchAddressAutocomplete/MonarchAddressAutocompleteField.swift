/// One field of the address form of a ``MonarchAddressAutocomplete``, for
/// errors.
@_spi(Experimental) public enum MonarchAddressAutocompleteField: Sendable, Hashable, CaseIterable {
    /// Street and number.
    case line1
    /// Flat, suite, floor or building.
    case line2
    /// City or town.
    case city
    /// State, province or region.
    case region
    /// Postal or ZIP code.
    case postalCode
    /// Country.
    case country
}
