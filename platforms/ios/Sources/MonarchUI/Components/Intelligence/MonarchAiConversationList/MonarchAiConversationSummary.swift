import Foundation

/// One conversation in the history of ``MonarchAiConversationList``.
public struct MonarchAiConversationSummary: Identifiable, Sendable, Hashable {
    /// The conversation's id, passed back to every callback.
    public var id: String
    /// The title. Empty shows "New conversation".
    public var title: String
    /// When it last changed, which decides its group.
    public var updatedAt: Date
    /// Pinned conversations are listed first, in their own group.
    public var isPinned: Bool
    /// A line of the latest message, shown under the title.
    public var preview: String?

    /// A conversation summary.
    ///
    /// - Parameters:
    ///   - id: The conversation's id.
    ///   - title: The title, already localised. Empty shows "New conversation".
    ///   - updatedAt: When it last changed.
    ///   - isPinned: Whether it is pinned.
    ///   - preview: A line of the latest message.
    public init(id: String, title: String, updatedAt: Date, isPinned: Bool = false, preview: String? = nil) {
        self.id = id
        self.title = title
        self.updatedAt = updatedAt
        self.isPinned = isPinned
        self.preview = preview
    }

    /// A conversation summary made from the messages of the conversation
    /// model: the last message's time and the text of the last message that
    /// has text become `updatedAt` and `preview`.
    ///
    /// - Parameters:
    ///   - id: The conversation's id.
    ///   - title: The title, already localised.
    ///   - messages: The conversation's messages, oldest first.
    ///   - isPinned: Whether it is pinned.
    public init(id: String, title: String, messages: [MonarchAiConversationMessage], isPinned: Bool = false) {
        self.init(
            id: id,
            title: title,
            updatedAt: messages.last?.createdDate ?? .distantPast,
            isPinned: isPinned,
            preview: MonarchAiConversationListTokens.preview(messages)
        )
    }
}
