package dev.monarchui.components.intelligence.aiconversation

/**
 * Reconnects to a run by its id, for example after the app was restarted mid-reply.
 *
 * @property runId the id from the reply's `Run` event.
 * @property lastEventId the last event id the client saw, when the service numbers its events.
 * @property conversationId the id of the conversation on the service, once known.
 * @property messages the visible conversation before the reply.
 */
public data class MonarchAiResumeRequest(
    public val runId: String,
    public val lastEventId: String? = null,
    public val conversationId: String? = null,
    public val messages: List<MonarchAiConversationMessage> = emptyList(),
)
