/// A field whose surviving value the person picks in a ``MonarchMergeDialog``.
public struct MonarchMergeField: Identifiable, Hashable, Sendable {
    /// The field identifier, the key in ``MonarchMergeRecord/values``.
    public let id: String
    /// The field's name, such as "Email".
    public let label: String

    /// A field.
    ///
    /// - Parameters:
    ///   - id: The key in each record's values.
    ///   - label: The field's name, already localised.
    public init(id: String, label: String) {
        self.id = id
        self.label = label
    }
}
