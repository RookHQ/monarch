package com.rookhq.monarch.components.intelligence.aiconversation

/**
 * Text of a message.
 *
 * @property text the text so far.
 * @property state whether more text is arriving.
 */
public data class MonarchAiTextPart(
    public val text: String,
    public val state: MonarchAiPartState = MonarchAiPartState.Done,
) : MonarchAiPart {
    override val type: String get() = "text"
}
