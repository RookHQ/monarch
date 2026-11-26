package com.rookhq.monarch.components.intelligence.aiconversation

/** What a transport streams back, one event at a time. The same events as the web's stream protocol. */
public sealed interface MonarchAiStreamEvent {
    /** The agent that is answering. */
    public data class Agent(public val agent: MonarchAiAgentRef) : MonarchAiStreamEvent

    /** More text of the answer. */
    public data class TextDelta(public val text: String) : MonarchAiStreamEvent

    /** More reasoning. */
    public data class ReasoningDelta(public val text: String) : MonarchAiStreamEvent

    /** The reasoning ended. [redacted] marks reasoning whose text is not shown. */
    public data class ReasoningEnd(
        public val durationMs: Long? = null,
        public val redacted: Boolean = false,
    ) : MonarchAiStreamEvent

    /** Appends a whole part: a tool call, source, citation, artifact, plan and so on. */
    public data class Part(public val part: MonarchAiPart) : MonarchAiStreamEvent

    /** Updates the tool call with this id, for example its state and output. */
    public data class ToolUpdate(public val id: String, public val patch: MonarchAiToolCallPatch) : MonarchAiStreamEvent

    /** A piece of a tool call's arguments as raw JSON text, appended to the call's `inputText`. */
    public data class ToolInputDelta(public val id: String, public val delta: String) : MonarchAiStreamEvent

    /** Why the model stopped. Sets the message's `finishReason`. */
    public data class Finish(public val reason: MonarchAiFinishReason) : MonarchAiStreamEvent

    /** The service's id for this run, so it can be resumed. Sets the message's `runId`. */
    public data class Run(public val id: String) : MonarchAiStreamEvent

    /** The token counts of the turn, and the model if the service says. */
    public data class Usage(public val usage: MonarchAiUsage, public val model: String? = null) : MonarchAiStreamEvent

    /** The assistant used, saved or forgot a memory. The same id and action again updates it. */
    public data class Memory(public val memory: MonarchAiMemoryPart) : MonarchAiStreamEvent

    /** The reply is complete. A [conversationId] replaces the conversation's id. */
    public data class Done(public val conversationId: String? = null) : MonarchAiStreamEvent

    /**
     * The turn failed. Without [retryable] the code decides
     * (`MonarchAiConversationError.isRetryable`); unknown codes are not retryable.
     */
    public data class Error(
        public val message: String,
        public val code: String? = null,
        public val retryable: Boolean? = null,
        public val retryAfterMs: Long? = null,
        public val resumable: Boolean = false,
    ) : MonarchAiStreamEvent
}
