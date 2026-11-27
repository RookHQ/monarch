package com.rookhq.monarch.components.intelligence.aiconversation

/**
 * The model's reasoning before or between its answers.
 *
 * @property text the reasoning so far.
 * @property state whether more reasoning is arriving.
 * @property durationMs how long the model reasoned, in milliseconds, once known.
 * @property redacted whether the reasoning exists but its text is not shown.
 */
public data class MonarchAiReasoningPart(
    public val text: String,
    public val state: MonarchAiPartState = MonarchAiPartState.Done,
    public val durationMs: Long? = null,
    public val redacted: Boolean = false,
) : MonarchAiPart {
    override val type: String get() = "reasoning"
}
