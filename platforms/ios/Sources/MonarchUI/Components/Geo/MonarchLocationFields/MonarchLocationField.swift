/// One field of ``MonarchLocationFields``.
public enum MonarchLocationField: String, CaseIterable, Hashable, Sendable {
    /// The country.
    case country
    /// The state, province or other first-level region.
    case region
    /// The city or town.
    case city
    /// The postal code.
    case postalCode
}
