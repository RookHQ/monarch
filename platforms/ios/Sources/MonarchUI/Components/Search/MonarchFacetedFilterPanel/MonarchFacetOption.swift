/// One choice of an options facet in a ``MonarchFacetedFilterPanel``.
public struct MonarchFacetOption: Identifiable, Hashable, Sendable {
    /// The value put in the selection when the choice is checked. Unique within the facet.
    public let id: String
    /// The visible label, already localised.
    public let label: String
    /// How many items match the choice. Shown after the label and read with it.
    public let count: Int?
    /// Whether the choice cannot be changed.
    public let isDisabled: Bool

    /// A choice.
    ///
    /// - Parameters:
    ///   - id: The value put in the selection when the choice is checked.
    ///   - label: The visible label, already localised.
    ///   - count: How many items match the choice.
    ///   - isDisabled: Whether the choice cannot be changed.
    public init(id: String, label: String, count: Int? = nil, isDisabled: Bool = false) {
        self.id = id
        self.label = label
        self.count = count
        self.isDisabled = isDisabled
    }
}
