package com.rookhq.monarch.components.intelligence.aiconversationlist

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationMessage

/**
 * One conversation of a `MonarchAiConversationList`.
 *
 * @property id the stable id, passed to the callbacks.
 * @property title the title. An empty or white-space title shows "New conversation".
 * @property updatedAtMillis when it was last updated, in epoch milliseconds (UTC). Decides its group.
 * @property pinned whether it is pinned to the top.
 * @property preview a line from the conversation, shown under the title.
 */
@Immutable
public data class MonarchAiConversationSummary(
    public val id: String,
    public val title: String,
    public val updatedAtMillis: Long,
    public val pinned: Boolean = false,
    public val preview: String? = null,
) {
    /**
     * A summary made from the conversation's messages: the time of the last message, and the text of the last
     * message that has text, on one line. Without messages it is dated at the epoch.
     *
     * @param id the stable id.
     * @param title the title.
     * @param messages the messages of the conversation, oldest first.
     * @param pinned whether it is pinned to the top.
     */
    public constructor(
        id: String,
        title: String,
        messages: List<MonarchAiConversationMessage>,
        pinned: Boolean = false,
    ) : this(
        id = id,
        title = title,
        updatedAtMillis = messages.lastOrNull()?.createdAtMillis ?: 0L,
        pinned = pinned,
        preview = messages.lastOrNull { it.text.isNotBlank() }?.text?.let(::oneLine),
    )

    private companion object {
        fun oneLine(text: String): String = text.trim().replace(Whitespace, " ")

        val Whitespace = Regex("\\s+")
    }
}
