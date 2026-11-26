/// One source the assistant can ground its answers in: a space, a document
/// or an app. A source with children is a group, such as a space holding
/// documents.
public struct MonarchAiKnowledgeSource: Identifiable, Sendable, Hashable, Codable {
    /// A stable id, listed in the selection when the source is selected.
    public var id: String
    /// The name shown in the tree, such as "Handbook". Already localised.
    public var label: String
    /// What the source is.
    public var kind: MonarchAiKnowledgeSourceKind
    /// A short second line, such as "24 documents". Already localised.
    public var description: String?
    /// The sources inside this one. Empty for a single source.
    public var children: [MonarchAiKnowledgeSource]

    /// A knowledge source.
    ///
    /// - Parameters:
    ///   - id: A stable id, listed in the selection when the source is selected.
    ///   - label: The name shown in the tree.
    ///   - kind: What the source is.
    ///   - description: A short second line.
    ///   - children: The sources inside this one.
    public init(
        id: String,
        label: String,
        kind: MonarchAiKnowledgeSourceKind,
        description: String? = nil,
        children: [MonarchAiKnowledgeSource] = []
    ) {
        self.id = id
        self.label = label
        self.kind = kind
        self.description = description
        self.children = children
    }
}
