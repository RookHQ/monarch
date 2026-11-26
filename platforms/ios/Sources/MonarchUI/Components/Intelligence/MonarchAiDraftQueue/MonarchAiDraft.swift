/// One AI-written draft waiting for review in ``MonarchAiDraftQueue``.
public struct MonarchAiDraft: Identifiable, Sendable, Hashable {
    /// A stable id, passed back to every callback.
    public var id: String
    /// What the draft is, such as the recipient or the subject. Already localised.
    public var title: String
    /// The drafted text, shown as plain text.
    public var body: String
    /// What it answers or is based on, such as the original message.
    public var context: String?
    /// Where it is in review.
    public var status: MonarchAiDraftStatus

    /// A draft.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - title: What the draft is, already localised.
    ///   - body: The drafted text.
    ///   - context: What it answers or is based on.
    ///   - status: Where it is in review.
    public init(id: String, title: String, body: String, context: String? = nil, status: MonarchAiDraftStatus = .draft)
    {
        self.id = id
        self.title = title
        self.body = body
        self.context = context
        self.status = status
    }

    /// A draft made from an assistant message of the conversation model:
    /// the message's id and text become the draft's id and body.
    ///
    /// - Parameters:
    ///   - message: The assistant message that holds the draft.
    ///   - title: What the draft is, already localised.
    ///   - context: What it answers or is based on.
    ///   - status: Where it is in review.
    public init(
        _ message: MonarchAiConversationMessage,
        title: String,
        context: String? = nil,
        status: MonarchAiDraftStatus = .draft
    ) {
        self.init(id: message.id, title: title, body: message.text, context: context, status: status)
    }
}
