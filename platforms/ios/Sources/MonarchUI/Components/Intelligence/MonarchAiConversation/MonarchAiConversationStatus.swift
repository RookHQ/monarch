/// What a conversation is doing.
public enum MonarchAiConversationStatus: String, Sendable, Hashable, CaseIterable {
    /// Nothing is running.
    case idle
    /// Sent, waiting for the first event.
    case submitted
    /// Events are arriving.
    case streaming
    /// A tool call waits for the person to approve or deny it.
    case awaitingApproval = "awaiting-approval"
    /// The model called a tool the client runs. Send the result with
    /// `submitToolResult(_:_:)`, or set `onToolCall`.
    case awaitingToolResult = "awaiting-tool-result"
    /// The last turn failed. `error` holds why.
    case error
    /// The person stopped the reply.
    case stopped
}
