/// What ``MonarchTimezoneSelect`` knows about one IANA zone at a given instant.
public struct MonarchTimezoneInfo: Sendable, Hashable {
    /// The IANA id, such as `Asia/Kolkata`.
    public var id: String
    /// The last part of the id, readable: `Kolkata`, `New York`.
    public var city: String
    /// The first part of the id: `Asia`, `America`. Empty for `UTC`.
    public var region: String
    /// The offset from UTC in seconds at the instant, daylight saving included.
    public var offsetSeconds: Int
    /// The offset as text: `UTC+05:30`.
    public var offsetLabel: String
    /// The localised long name, such as "India Standard Time". Empty when the system has none.
    public var longName: String

    /// Zone information.
    ///
    /// - Parameters:
    ///   - id: The IANA id.
    ///   - city: The last part of the id, readable.
    ///   - region: The first part of the id.
    ///   - offsetSeconds: The offset from UTC in seconds.
    ///   - offsetLabel: The offset as text.
    ///   - longName: The localised long name.
    public init(id: String, city: String, region: String, offsetSeconds: Int, offsetLabel: String, longName: String) {
        self.id = id
        self.city = city
        self.region = region
        self.offsetSeconds = offsetSeconds
        self.offsetLabel = offsetLabel
        self.longName = longName
    }
}
