/// One choice of a select ``MonarchEditableCell``.
public struct MonarchEditableCellOption: Identifiable, Sendable, Hashable {
    /// The stored value, written to the cell as `.text(value)`.
    public let value: String
    /// The visible label, already localised.
    public let label: String
    /// Whether the choice is shown but cannot be chosen.
    public let isDisabled: Bool

    /// The identity of the option: its value.
    public var id: String { value }

    /// A choice.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - value: The stored value.
    ///   - isDisabled: Whether the choice is shown but cannot be chosen.
    public init(_ label: String, value: String, isDisabled: Bool = false) {
        self.label = label
        self.value = value
        self.isDisabled = isDisabled
    }
}
