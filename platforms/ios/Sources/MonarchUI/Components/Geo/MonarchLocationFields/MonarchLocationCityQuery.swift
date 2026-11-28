/// What a city search of ``MonarchLocationFields`` asks its adapter for.
public struct MonarchLocationCityQuery: Hashable, Sendable {
    /// The chosen country's code.
    public let country: String
    /// The chosen region's code, or `nil` to search the whole country.
    public let region: String?
    /// The typed text, trimmed. Empty when a chosen region's cities are listed.
    public let query: String

    /// A city search.
    ///
    /// - Parameters:
    ///   - country: The chosen country's code.
    ///   - region: The chosen region's code, or `nil`.
    ///   - query: The typed text, trimmed.
    public init(country: String, region: String?, query: String) {
        self.country = country
        self.region = region
        self.query = query
    }
}
