import Foundation

/// A value for one country in ``MonarchWorldMap``.
public struct MonarchWorldMapDatum: Sendable, Hashable {
    /// The ISO 3166-1 alpha-2 code, such as `DE`. Upper case, without spaces.
    public let country: String
    /// The value.
    public let value: Double
    /// Extra lines for the country, such as users and sessions.
    public let details: [MonarchWorldMapDetail]

    /// A country's value.
    ///
    /// - Parameters:
    ///   - country: The ISO 3166-1 alpha-2 code. Case and surrounding spaces do not matter.
    ///   - value: The value. A value that is not finite is left out.
    ///   - details: Extra lines read with the country and shown as table columns.
    public init(_ country: String, value: Double, details: [MonarchWorldMapDetail] = []) {
        self.country = country.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        self.value = value
        self.details = details
    }
}
