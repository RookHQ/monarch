/// A source as ``MonarchAiSourceList`` shows it: the source part of a reply,
/// whether the answer relies on it, and a short excerpt.
public struct MonarchAiSourceListItem: Identifiable, Sendable, Hashable {
    /// The source, as the conversation model holds it.
    public var source: MonarchAiSourcePart
    /// Whether the answer relies on the source. `false` for a source the
    /// assistant looked at but did not use, listed under "Considered".
    public var isUsed: Bool
    /// A short excerpt shown under the title.
    public var snippet: String?

    /// The id of the source.
    public var id: String { source.id }

    /// A source in the list.
    ///
    /// - Parameters:
    ///   - source: The source part of the reply.
    ///   - isUsed: Whether the answer relies on it. `false` lists it under "Considered".
    ///   - snippet: A short excerpt shown under the title.
    public init(_ source: MonarchAiSourcePart, isUsed: Bool = true, snippet: String? = nil) {
        self.source = source
        self.isUsed = isUsed
        self.snippet = snippet
    }

    /// The sources of a message, in the order they first appear, each once.
    /// This is the numbering a reply's citations use: the first is `[1]`.
    ///
    /// - Parameter message: A message of the conversation.
    /// - Returns: One used item per distinct source part.
    public static func sources(of message: MonarchAiConversationMessage) -> [MonarchAiSourceListItem] {
        var seen = Set<String>()
        var items: [MonarchAiSourceListItem] = []
        for part in message.parts {
            if case .source(let source) = part, seen.insert(source.id).inserted {
                items.append(MonarchAiSourceListItem(source))
            }
        }
        return items
    }
}
