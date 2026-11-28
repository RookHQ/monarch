/// What a turn sends to the transport.
///
/// There is no abort signal: the conversation reads the stream in a task and
/// cancels it when the person stops the reply. Stop work in the stream's
/// `onTermination` handler.
public struct MonarchAiTransportRequest: Sendable, Hashable {
    /// The visible conversation up to and including the new user message.
    public var messages: [MonarchAiConversationMessage]
    /// Why the turn is asked for.
    public var trigger: MonarchAiTrigger
    /// The id of the conversation on the service, once known.
    public var conversationId: String?
    /// Extra request fields: the conversation's `body` merged under the fields given to `send`.
    public var body: [String: MonarchAiJSON]

    /// A request.
    ///
    /// - Parameters:
    ///   - messages: The visible conversation up to and including the new user message.
    ///   - trigger: Why the turn is asked for.
    ///   - conversationId: The id of the conversation on the service.
    ///   - body: Extra request fields.
    public init(
        messages: [MonarchAiConversationMessage],
        trigger: MonarchAiTrigger,
        conversationId: String? = nil,
        body: [String: MonarchAiJSON] = [:]
    ) {
        self.messages = messages
        self.trigger = trigger
        self.conversationId = conversationId
        self.body = body
    }
}
