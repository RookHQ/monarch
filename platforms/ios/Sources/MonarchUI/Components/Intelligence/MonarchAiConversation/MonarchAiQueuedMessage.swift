/// A message written while offline, waiting to be sent.
public struct MonarchAiQueuedMessage: Identifiable, Sendable, Hashable {
    /// The id of the user message it becomes. Pass it to `cancelQueued(_:)`.
    public var id: String
    /// The text.
    public var text: String
    /// The attached files.
    public var files: [MonarchAiFilePart]
    /// The extra request fields given to `send`.
    public var body: [String: MonarchAiJSON]
    /// The ISO 8601 time it was written.
    public var createdAt: String

    /// A queued message.
    ///
    /// - Parameters:
    ///   - id: The id of the user message it becomes.
    ///   - text: The text.
    ///   - files: The attached files.
    ///   - body: The extra request fields given to `send`.
    ///   - createdAt: The ISO 8601 time it was written.
    public init(
        id: String,
        text: String,
        files: [MonarchAiFilePart] = [],
        body: [String: MonarchAiJSON] = [:],
        createdAt: String
    ) {
        self.id = id
        self.text = text
        self.files = files
        self.body = body
        self.createdAt = createdAt
    }

    /// The user message the queued message will become, so a thread can show it.
    var message: MonarchAiConversationMessage {
        var parts: [MonarchAiPart] = text.isEmpty ? [] : [.text(MonarchAiTextPart(text: text))]
        parts += files.map(MonarchAiPart.file)
        return MonarchAiConversationMessage(id: id, role: .user, parts: parts, createdAt: createdAt, isQueued: true)
    }
}
