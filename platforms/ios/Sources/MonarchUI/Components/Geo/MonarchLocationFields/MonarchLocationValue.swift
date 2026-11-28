/// An address's country, region, city and postal code, as edited by
/// ``MonarchLocationFields``.
///
/// The region and the city each keep a code from the list (when one was
/// chosen) and the text people read, so a typed value is never lost.
public struct MonarchLocationValue: Equatable, Hashable, Sendable {
    /// The country's code, such as "US", or `nil`.
    public var country: String?
    /// The chosen region's code from the list, or `nil` when it was typed or not chosen.
    public var region: String?
    /// The region's text: the chosen region's name or what was typed.
    public var regionName: String
    /// The city's text: the chosen city's name or what was typed.
    public var city: String
    /// The chosen city's code from the list, or `nil` when it was typed.
    public var cityID: String?
    /// The postal code as typed.
    public var postalCode: String

    /// An address. Every part is empty by default.
    ///
    /// - Parameters:
    ///   - country: The country's code, such as "US".
    ///   - region: The region's code from the list.
    ///   - regionName: The region's name or typed text.
    ///   - city: The city's name or typed text.
    ///   - cityID: The city's code from the list.
    ///   - postalCode: The postal code.
    public init(
        country: String? = nil,
        region: String? = nil,
        regionName: String = "",
        city: String = "",
        cityID: String? = nil,
        postalCode: String = ""
    ) {
        self.country = country
        self.region = region
        self.regionName = regionName
        self.city = city
        self.cityID = cityID
        self.postalCode = postalCode
    }
}
