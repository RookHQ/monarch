/// A country from a ``MonarchLocationAdapter``: a stable code and the name people read.
public struct MonarchLocationOption: Identifiable, Hashable, Sendable {
    /// The code to store, such as an ISO 3166-1 code or the source's own id.
    public let id: String
    /// The name shown in the list, already localised.
    public let label: String
    /// Secondary text in the list.
    public let description: String?

    /// A country option.
    ///
    /// - Parameters:
    ///   - id: The code to store.
    ///   - label: The name shown in the list.
    ///   - description: Secondary text in the list.
    public init(id: String, label: String, description: String? = nil) {
        self.id = id
        self.label = label
        self.description = description
    }
}
