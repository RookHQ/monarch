/// A node of a ``MonarchForceGraph``.
public struct MonarchForceGraphNode: Sendable, Hashable, Identifiable {
    /// A stable id, referenced by edges.
    public let id: String
    /// The name drawn under the node, in the table and for VoiceOver, already localised.
    public let label: String
    /// The key of the node's group, which sets its colour.
    public let group: String?
    /// One line shown when the node is selected, such as its type, already localised.
    public let description: String?

    /// A node.
    ///
    /// - Parameters:
    ///   - label: The name, already localised.
    ///   - id: A stable id. The label when `nil`.
    ///   - group: The key of the node's group.
    ///   - description: One line shown when the node is selected.
    public init(_ label: String, id: String? = nil, group: String? = nil, description: String? = nil) {
        self.label = label
        self.id = id ?? label
        self.group = group
        self.description = description
    }
}
