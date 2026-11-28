package dev.monarchui.components.intelligence.aiconversation

/**
 * One turn of a conversation: the vendor-neutral message model every Monarch
 * AI component renders. It has the same fields as the web's `AiMessage` and
 * reads and writes the same JSON ([toJson], [fromJson]), so a stored
 * conversation can move between platforms.
 *
 * The web type is `AiMessage`; the name follows iOS, where `MonarchAiMessage` is the message view.
 *
 * @property id the id of the message.
 * @property role who wrote it.
 * @property parts the parts, in the order the model produced them.
 * @property createdAt the ISO 8601 time the message was created, in UTC with milliseconds.
 * @property agent the agent that answered.
 * @property model the model that produced it, for example `gemini-2.5-pro`.
 * @property usage the token counts and cost of the turn.
 * @property feedback the feedback a person gave.
 * @property branch set when the message has alternatives: which one is shown.
 * @property finishReason why the model stopped. `Length` means the answer was cut short.
 * @property runId the service's id for the run that wrote this reply, used to resume it.
 * @property queued whether this is a user message written while offline that has not been sent yet.
 */
public data class MonarchAiConversationMessage(
    public val id: String,
    public val role: MonarchAiRole,
    public val parts: List<MonarchAiPart>,
    public val createdAt: String,
    public val agent: MonarchAiAgentRef? = null,
    public val model: String? = null,
    public val usage: MonarchAiUsage? = null,
    public val feedback: MonarchAiMessageFeedback? = null,
    public val branch: MonarchAiBranchPosition? = null,
    public val finishReason: MonarchAiFinishReason? = null,
    public val runId: String? = null,
    public val queued: Boolean = false,
) {
    /** The plain text of the message: its text parts joined. Reasoning and tool parts are left out. */
    public val text: String
        get() = parts.filterIsInstance<MonarchAiTextPart>().joinToString(separator = "") { it.text }

    /** Whether any text or reasoning part of the message is still streaming. */
    public val streaming: Boolean
        get() = parts.any {
            (it is MonarchAiTextPart && it.state == MonarchAiPartState.Streaming) ||
                (it is MonarchAiReasoningPart && it.state == MonarchAiPartState.Streaming)
        }

    /** The tool calls of the message, in order. */
    public val toolCalls: List<MonarchAiToolCallPart>
        get() = parts.filterIsInstance<MonarchAiToolCallPart>()

    /** The memories the reply used, saved and forgot, in order. */
    public val memories: List<MonarchAiMemoryPart>
        get() = parts.filterIsInstance<MonarchAiMemoryPart>()

    /** [createdAt] in epoch milliseconds (UTC), or `null` if it is not an ISO 8601 time. */
    public val createdAtMillis: Long?
        get() = MonarchAiTimestamp.millis(createdAt)

    /** The message as the web's JSON object. `queued` and other empty fields are left out, as the web does. */
    public fun toJson(): MonarchAiJSON = MonarchAiMessageCodec.encode(this)

    /** Reads and writes messages as the web's JSON, and the times the model uses. */
    public companion object {
        /**
         * A message from the web's JSON object.
         *
         * @throws IllegalArgumentException when a required field is missing or a part has an unknown type.
         */
        public fun fromJson(json: MonarchAiJSON): MonarchAiConversationMessage = MonarchAiMessageCodec.decode(json)

        /**
         * Messages from JSON text holding an array of the web's message objects, for example a stored
         * conversation.
         *
         * @throws IllegalArgumentException when the text is not such an array.
         */
        public fun listFromJson(text: String): List<MonarchAiConversationMessage> {
            val array = MonarchAiJSON.parse(text)
            require(array is MonarchAiJSON.Array) { "Expected a JSON array of messages" }
            return array.values.map(::fromJson)
        }

        /** JSON text holding an array of the web's message objects. */
        public fun listToJson(messages: List<MonarchAiConversationMessage>): String =
            MonarchAiJSON.Array(messages.map { it.toJson() }).toJsonString()

        /** The ISO 8601 text of a time in epoch milliseconds, as [createdAt] and the tool call times hold it. */
        public fun timestamp(millis: Long): String = MonarchAiTimestamp.text(millis)
    }
}
