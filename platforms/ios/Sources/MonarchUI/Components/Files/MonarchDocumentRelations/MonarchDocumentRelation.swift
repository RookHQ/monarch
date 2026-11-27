/// One relation of a document, such as "Parent of Q3 plan".
@_spi(Experimental) public struct MonarchDocumentRelation: Identifiable, Sendable, Hashable {
    /// A stable id.
    public let id: String
    /// The `value` of a ``MonarchDocumentRelationType``.
    public let type: String
    /// Whether it reads from this document or from the target.
    public let direction: MonarchDocumentRelationDirection
    /// The document at the other end.
    public let target: MonarchDocumentRelationTarget

    /// A relation.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - type: The `value` of a relation type.
    ///   - direction: `.outgoing` reads as the type, `.incoming` as its inverse.
    ///   - target: The document at the other end.
    public init(
        id: String,
        type: String,
        direction: MonarchDocumentRelationDirection = .outgoing,
        target: MonarchDocumentRelationTarget
    ) {
        self.id = id
        self.type = type
        self.direction = direction
        self.target = target
    }
}
