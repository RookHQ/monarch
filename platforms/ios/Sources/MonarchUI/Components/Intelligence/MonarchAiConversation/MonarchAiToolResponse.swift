/// A person's or a policy's answer to a tool call that waited for approval.
public struct MonarchAiToolResponse: Sendable, Hashable {
    /// The id of the tool call.
    public var toolCallId: String
    /// Whether the call was approved.
    public var isApproved: Bool
    /// The edited arguments, when the person changed them before approving.
    public var input: MonarchAiJSON?
    /// Why the call was denied.
    public var reason: String?
    /// The visible conversation.
    public var messages: [MonarchAiConversationMessage]
    /// The id of the conversation on the service, once known.
    public var conversationId: String?

    /// A tool response.
    ///
    /// - Parameters:
    ///   - toolCallId: The id of the tool call.
    ///   - isApproved: Whether the call was approved.
    ///   - input: The edited arguments.
    ///   - reason: Why the call was denied.
    ///   - messages: The visible conversation.
    ///   - conversationId: The id of the conversation on the service.
    public init(
        toolCallId: String,
        isApproved: Bool,
        input: MonarchAiJSON? = nil,
        reason: String? = nil,
        messages: [MonarchAiConversationMessage] = [],
        conversationId: String? = nil
    ) {
        self.toolCallId = toolCallId
        self.isApproved = isApproved
        self.input = input
        self.reason = reason
        self.messages = messages
        self.conversationId = conversationId
    }
}
