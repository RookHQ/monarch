package dev.monarchui.components.intelligence.aidraftqueue

import androidx.compose.runtime.Immutable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationMessage

/**
 * One AI-written draft waiting for review in `MonarchAiDraftQueue`.
 *
 * @property id a stable id, passed back to every callback.
 * @property title what the draft is, such as the recipient or the subject, already localised.
 * @property body the drafted text, shown as plain text.
 * @property context what it answers or is based on, such as the original message. `null` for none.
 * @property status where it is in review.
 */
@Immutable
public data class MonarchAiDraft(
    public val id: String,
    public val title: String,
    public val body: String,
    public val context: String? = null,
    public val status: MonarchAiDraftStatus = MonarchAiDraftStatus.Draft,
) {
    /**
     * A draft made from an assistant message of the conversation model: the
     * message's id and text become the draft's id and body.
     *
     * @param message the assistant message that holds the draft.
     * @param title what the draft is, already localised.
     * @param context what it answers or is based on.
     * @param status where it is in review.
     */
    public constructor(
        message: MonarchAiConversationMessage,
        title: String,
        context: String? = null,
        status: MonarchAiDraftStatus = MonarchAiDraftStatus.Draft,
    ) : this(message.id, title, message.text, context, status)
}
