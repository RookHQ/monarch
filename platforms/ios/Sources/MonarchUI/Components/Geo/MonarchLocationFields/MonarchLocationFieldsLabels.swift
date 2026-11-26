/// Replacements for the built-in strings of ``MonarchLocationFields``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchLocationFieldsLabels: Sendable {
    /// The country field's label. Built in: "Country".
    public var country: String?
    /// The region field's label when neither the adapter nor Monarch knows the country's word. Built in: "State or region".
    public var region: String?
    /// The city field's label. Built in: "City".
    public var city: String?
    /// The postal code field's label for every country. Built in: the country's word, such as "ZIP code" for US, or "Postal code".
    public var postalCode: String?
    /// Shown when the countries could not be loaded. Built in: "Couldn’t load the countries."
    public var countriesLoadFailed: String?
    /// Shown when the regions could not be loaded. Built in by ``MonarchRegionSelect``.
    public var regionsLoadFailed: String?
    /// Shown in the city list when the search failed. Built in: "Couldn’t load the cities."
    public var citiesLoadFailed: String?
    /// The prompt of the region and city fields while no country is chosen. Built in: "Choose a country first".
    public var chooseCountryFirst: String?
    /// Shown in the city list until enough letters are typed. Built in: "Type at least 2 letters to search."
    public var typeMore: (@Sendable (Int) -> String)?
    /// Shown in the city list when nothing matches. Built in: "No cities match".
    public var noMatch: String?
    /// The row that keeps the typed city as it is. Built in: "Use “Paris”".
    public var useTyped: (@Sendable (String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        country: String? = nil,
        region: String? = nil,
        city: String? = nil,
        postalCode: String? = nil,
        countriesLoadFailed: String? = nil,
        regionsLoadFailed: String? = nil,
        citiesLoadFailed: String? = nil,
        chooseCountryFirst: String? = nil,
        typeMore: (@Sendable (Int) -> String)? = nil,
        noMatch: String? = nil,
        useTyped: (@Sendable (String) -> String)? = nil
    ) {
        self.country = country
        self.region = region
        self.city = city
        self.postalCode = postalCode
        self.countriesLoadFailed = countriesLoadFailed
        self.regionsLoadFailed = regionsLoadFailed
        self.citiesLoadFailed = citiesLoadFailed
        self.chooseCountryFirst = chooseCountryFirst
        self.typeMore = typeMore
        self.noMatch = noMatch
        self.useTyped = useTyped
    }
}
