/// A choice of a select, multi-select, radio or checkboxes question of a ``MonarchFormFieldDefinition``.
public struct MonarchFormFieldOption: Identifiable, Hashable, Sendable {
    /// The value stored in the answer.
    public var value: String
    /// The visible label, already localised.
    public var label: String

    /// The value, which identifies the option.
    public var id: String { value }

    /// A choice.
    ///
    /// - Parameters:
    ///   - value: The value stored in the answer.
    ///   - label: The visible label, already localised.
    public init(value: String, label: String) {
        self.value = value
        self.label = label
    }
}
