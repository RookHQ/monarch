package com.rookhq.monarch.components.intelligence.aiconversation

/**
 * A person's or a policy's answer to a tool call that waited for approval.
 *
 * @property toolCallId the id of the tool call.
 * @property approved whether the call was approved.
 * @property input the edited arguments, when the person changed them before approving.
 * @property reason why the call was denied.
 * @property messages the visible conversation.
 * @property conversationId the id of the conversation on the service, once known.
 */
public data class MonarchAiToolResponse(
    public val toolCallId: String,
    public val approved: Boolean,
    public val input: MonarchAiJSON? = null,
    public val reason: String? = null,
    public val messages: List<MonarchAiConversationMessage> = emptyList(),
    public val conversationId: String? = null,
)
