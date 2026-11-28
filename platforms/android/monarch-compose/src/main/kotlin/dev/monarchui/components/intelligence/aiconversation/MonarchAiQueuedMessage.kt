package dev.monarchui.components.intelligence.aiconversation

/**
 * A message written while offline, waiting to be sent.
 *
 * @property id the id of the user message it becomes. Pass it to `cancelQueued`.
 * @property text the text.
 * @property files the attached files.
 * @property body the extra request fields given to `send`.
 * @property createdAt the ISO 8601 time it was written.
 */
public data class MonarchAiQueuedMessage(
    public val id: String,
    public val text: String,
    public val createdAt: String,
    public val files: List<MonarchAiFilePart> = emptyList(),
    public val body: Map<String, MonarchAiJSON> = emptyMap(),
) {
    /** The user message the queued message will become, so a thread can show it. */
    internal val message: MonarchAiConversationMessage
        get() = MonarchAiConversationMessage(
            id = id,
            role = MonarchAiRole.User,
            parts = (if (text.isEmpty()) emptyList() else listOf(MonarchAiTextPart(text))) + files,
            createdAt = createdAt,
            queued = true,
        )
}
