/// A choice in the add form of ``MonarchTimeOffPanel``: a person in "Who",
/// or a kind in "Type".
@_spi(Experimental) public struct MonarchTimeOffOption: Identifiable, Sendable, Hashable {
    /// The stored value: a person's id or a type's key.
    public let value: String
    /// The visible label, already localised.
    public let label: String

    /// The identity of the option: its value.
    public var id: String { value }

    /// A choice.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - value: The stored value.
    public init(_ label: String, value: String) {
        self.label = label
        self.value = value
    }
}
