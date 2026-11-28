/// One kind of optional storage or processing a product uses, by its id and
/// the name people see.
public struct MonarchConsentCategory: Sendable, Hashable, Identifiable {
    /// The stable id, such as `analytics`.
    public let id: String
    /// The name people see, already localised, such as "Analytics".
    public let label: String

    /// A consent category.
    ///
    /// - Parameters:
    ///   - id: The stable id stored in records.
    ///   - label: The name people see, already localised.
    public init(id: String, label: String) {
        self.id = id
        self.label = label
    }
}
