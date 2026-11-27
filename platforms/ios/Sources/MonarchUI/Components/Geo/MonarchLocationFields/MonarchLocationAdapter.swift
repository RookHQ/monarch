/// Where ``MonarchLocationFields`` gets its lists.
///
/// Monarch makes no network calls: the app implements each closure against
/// its own service. A closure left `nil` uses Monarch's built-in behaviour:
/// every ISO 3166-1 country named by Foundation, the built-in region lists
/// of ``MonarchRegionSelect`` (with a text field for other countries), and
/// a text field for the city. Each call runs in a task that is cancelled
/// when its answer is no longer needed.
public struct MonarchLocationAdapter: Sendable {
    /// Loads the countries. `nil` lists every country by its Foundation name.
    public var countries: (@Sendable () async throws -> [MonarchLocationOption])?
    /// Loads a country's regions. Return no regions for a text field. `nil` uses the built-in lists.
    public var regions: (@Sendable (String) async throws -> MonarchLocationRegions)?
    /// Searches cities as people type. `nil` gives a text field.
    public var cities: (@Sendable (MonarchLocationCityQuery) async throws -> [MonarchLocationCity])?

    /// An adapter. Every closure is optional.
    ///
    /// - Parameters:
    ///   - countries: Loads the countries.
    ///   - regions: Loads a country's regions.
    ///   - cities: Searches cities.
    public init(
        countries: (@Sendable () async throws -> [MonarchLocationOption])? = nil,
        regions: (@Sendable (String) async throws -> MonarchLocationRegions)? = nil,
        cities: (@Sendable (MonarchLocationCityQuery) async throws -> [MonarchLocationCity])? = nil
    ) {
        self.countries = countries
        self.regions = regions
        self.cities = cities
    }
}
