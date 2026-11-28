package dev.monarchui.components.intelligence.aiconversation

/**
 * What a turn sends to the transport. There is no abort signal: the
 * conversation cancels the collection of the flow when the person stops.
 *
 * @property messages the visible conversation up to and including the new user message.
 * @property trigger why the turn is asked for.
 * @property conversationId the id of the conversation on the service, once known.
 * @property body extra request fields: the conversation's `body` merged under the fields given to `send`.
 */
public data class MonarchAiTransportRequest(
    public val messages: List<MonarchAiConversationMessage>,
    public val trigger: MonarchAiTrigger,
    public val conversationId: String? = null,
    public val body: Map<String, MonarchAiJSON> = emptyMap(),
)
