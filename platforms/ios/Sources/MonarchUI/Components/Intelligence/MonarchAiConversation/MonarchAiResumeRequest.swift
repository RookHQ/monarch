/// Reconnects to a run by its id, for example after the app was relaunched mid-reply.
public struct MonarchAiResumeRequest: Sendable, Hashable {
    /// The id from the reply's `run` event.
    public var runId: String
    /// The last event id the client saw, when the service numbers its events.
    public var lastEventId: String?
    /// The id of the conversation on the service, once known.
    public var conversationId: String?
    /// The visible conversation before the reply.
    public var messages: [MonarchAiConversationMessage]

    /// A resume request.
    ///
    /// - Parameters:
    ///   - runId: The id from the reply's `run` event.
    ///   - lastEventId: The last event id the client saw.
    ///   - conversationId: The id of the conversation on the service.
    ///   - messages: The visible conversation before the reply.
    public init(
        runId: String,
        lastEventId: String? = nil,
        conversationId: String? = nil,
        messages: [MonarchAiConversationMessage] = []
    ) {
        self.runId = runId
        self.lastEventId = lastEventId
        self.conversationId = conversationId
        self.messages = messages
    }
}
