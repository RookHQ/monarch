/// What a ``MonarchAiMentionItem`` points at. Results are grouped by kind, in this order.
public enum MonarchAiMentionKind: String, Sendable, Hashable, Codable, CaseIterable {
    /// A document or file. Becomes a `file` context item.
    case doc
    /// A person, shown with an avatar.
    case person
    /// A record, such as a customer or an invoice.
    case record
    /// A connected app.
    case app

    /// The kind of context item it becomes.
    public var contextKind: MonarchAiContextKind {
        switch self {
        case .doc: .file
        case .person: .person
        case .record: .record
        case .app: .app
        }
    }
}
