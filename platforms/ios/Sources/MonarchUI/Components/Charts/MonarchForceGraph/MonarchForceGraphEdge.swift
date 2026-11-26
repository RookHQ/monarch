/// A link between two nodes of a ``MonarchForceGraph``. Links have no direction.
public struct MonarchForceGraphEdge: Sendable, Hashable {
    /// The id of one end.
    public let source: String
    /// The id of the other end.
    public let target: String
    /// The key of the link's kind, which sets its colour and dash.
    public let kind: String?

    /// A link.
    ///
    /// - Parameters:
    ///   - source: The id of one end.
    ///   - target: The id of the other end.
    ///   - kind: The key of the link's kind.
    public init(_ source: String, _ target: String, kind: String? = nil) {
        self.source = source
        self.target = target
        self.kind = kind
    }
}
