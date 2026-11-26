/// One kind of optional storage or processing in ``MonarchConsentPreferences``,
/// with what it is for and whether it can be switched off.
public struct MonarchConsentPreferencesCategory: Sendable, Hashable, Identifiable {
    /// The stable id stored in records, such as `analytics`.
    public let id: String
    /// The name people see, already localised, such as "Analytics".
    public let label: String
    /// What the category is used for, already localised.
    public let description: String
    /// Whether the category is strictly necessary: always on and cannot be switched off.
    public let isRequired: Bool
    /// Whether the category sells or shares personal data, so a Global Privacy Control signal keeps it off.
    public let isSaleOrSharing: Bool

    /// A consent category.
    ///
    /// - Parameters:
    ///   - id: The stable id stored in records.
    ///   - label: The name people see, already localised.
    ///   - description: What the category is used for, already localised.
    ///   - isRequired: Whether it is strictly necessary and always on.
    ///   - isSaleOrSharing: Whether it sells or shares personal data.
    public init(
        id: String,
        label: String,
        description: String,
        isRequired: Bool = false,
        isSaleOrSharing: Bool = false
    ) {
        self.id = id
        self.label = label
        self.description = description
        self.isRequired = isRequired
        self.isSaleOrSharing = isSaleOrSharing
    }
}
