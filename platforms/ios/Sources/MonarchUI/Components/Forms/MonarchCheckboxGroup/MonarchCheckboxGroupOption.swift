/// One choice of a ``MonarchCheckboxGroup``.
public struct MonarchCheckboxGroupOption: Identifiable, Hashable, Sendable {
    /// The value put in the selection when the choice is checked. Unique within the group.
    public let id: String
    /// The visible label, already localised. It also names the checkbox.
    public let label: String
    /// Secondary text under the label, read after it.
    public let description: String?
    /// Whether the choice cannot be changed. Select all leaves it as it is.
    public let isDisabled: Bool

    /// A choice.
    ///
    /// - Parameters:
    ///   - id: The value put in the selection when the choice is checked.
    ///   - label: The visible label, already localised.
    ///   - description: Secondary text under the label.
    ///   - isDisabled: Whether the choice cannot be changed.
    public init(id: String, label: String, description: String? = nil, isDisabled: Bool = false) {
        self.id = id
        self.label = label
        self.description = description
        self.isDisabled = isDisabled
    }
}
