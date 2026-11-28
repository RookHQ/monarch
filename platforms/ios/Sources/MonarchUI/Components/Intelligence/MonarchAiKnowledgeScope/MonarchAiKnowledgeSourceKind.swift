/// What a ``MonarchAiKnowledgeSource`` is. The raw values match the web.
public enum MonarchAiKnowledgeSourceKind: String, Sendable, Hashable, Codable, CaseIterable {
    /// A space or workspace that holds documents.
    case space
    /// A single document.
    case document = "doc"
    /// A connected app.
    case app
}
