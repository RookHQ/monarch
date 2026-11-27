import Foundation

/// A structured postal address, as filled in by ``MonarchAddressAutocomplete``.
@_spi(Experimental) public struct MonarchAddressAutocompleteAddress: Hashable, Sendable {
    /// Street and number.
    public var line1: String
    /// Flat, suite, floor or building.
    public var line2: String
    /// City or town.
    public var city: String
    /// State, province, county or region, as written.
    public var region: String
    /// Postal or ZIP code.
    public var postalCode: String
    /// The ISO 3166-1 alpha-2 country code, such as "PT", or `nil`.
    public var countryCode: String?

    /// An address. Every part is empty by default.
    ///
    /// - Parameters:
    ///   - line1: Street and number.
    ///   - line2: Flat, suite, floor or building.
    ///   - city: City or town.
    ///   - region: State, province, county or region.
    ///   - postalCode: Postal or ZIP code.
    ///   - countryCode: The ISO 3166-1 alpha-2 country code.
    public init(
        line1: String = "",
        line2: String = "",
        city: String = "",
        region: String = "",
        postalCode: String = "",
        countryCode: String? = nil
    ) {
        self.line1 = line1
        self.line2 = line2
        self.city = city
        self.region = region
        self.postalCode = postalCode
        self.countryCode = countryCode
    }

    /// Whether every part is empty.
    public var isEmpty: Bool {
        [line1, line2, city, region, postalCode].allSatisfy {
            $0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        } && countryCode == nil
    }
}
