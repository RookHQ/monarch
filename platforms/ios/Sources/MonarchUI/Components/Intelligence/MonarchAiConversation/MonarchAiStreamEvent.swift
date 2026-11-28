/// What a transport streams back, one event at a time.
public enum MonarchAiStreamEvent: Sendable, Hashable {
    /// The agent that is answering.
    case agent(MonarchAiAgentRef)
    /// More text of the answer.
    case textDelta(String)
    /// More reasoning.
    case reasoningDelta(String)
    /// The reasoning ended. `isRedacted` marks reasoning whose text is not shown.
    case reasoningEnd(durationMs: Int? = nil, isRedacted: Bool = false)
    /// Appends a whole part: a tool call, source, citation, artifact, plan and so on.
    case part(MonarchAiPart)
    /// Updates the tool call with this id, for example its state and output.
    case toolUpdate(id: String, patch: MonarchAiToolCallPatch)
    /// A piece of a tool call's arguments as raw JSON text, appended to the call's `inputText`.
    case toolInputDelta(id: String, delta: String)
    /// Why the model stopped. Sets the message's `finishReason`.
    case finish(MonarchAiFinishReason)
    /// The service's id for this run, so it can be resumed. Sets the message's `runId`.
    case run(id: String)
    /// The token counts of the turn, and the model if the service says.
    case usage(MonarchAiUsage, model: String? = nil)
    /// The assistant used, saved or forgot a memory. Adds a memory part to the
    /// reply; the same id and action again updates it.
    case memory(MonarchAiMemoryPart)
    /// The reply is complete.
    case done(conversationId: String? = nil)
    /// The turn failed. Without `isRetryable` the code decides
    /// (`MonarchAiConversationError.isRetryable(code:)`); unknown codes are not retryable.
    case error(
        code: String? = nil,
        message: String,
        isRetryable: Bool? = nil,
        retryAfterMs: Int? = nil,
        isResumable: Bool = false
    )
}
