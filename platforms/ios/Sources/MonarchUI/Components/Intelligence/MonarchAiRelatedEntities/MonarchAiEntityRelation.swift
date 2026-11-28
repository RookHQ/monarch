/// A link between two related entities, by id.
public struct MonarchAiEntityRelation: Sendable, Hashable {
    /// The id of one entity.
    public var from: String
    /// The id of the other entity.
    public var to: String
    /// How they relate, such as "works at". Already localised.
    public var label: String?

    /// A relation.
    ///
    /// - Parameters:
    ///   - from: The id of one entity.
    ///   - to: The id of the other entity.
    ///   - label: How they relate, such as "works at".
    public init(from: String, to: String, label: String? = nil) {
        self.from = from
        self.to = to
        self.label = label
    }
}
