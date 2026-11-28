package dev.monarchui.components.intelligence.aiconversation

/**
 * A failure of the turn, as the last part of the reply.
 *
 * @property code the error code, for example `network` or `http-503`. Never shown;
 * `MonarchAiErrorKind.fromCode` maps it to plain words.
 * @property message the message for the person.
 * @property retryable whether the turn may be tried again unchanged.
 * @property retryAfterMs milliseconds until a retry is allowed, when the service said.
 * @property resumable whether the connection dropped mid-reply but the run can be picked up where the
 * service has it, with `MonarchAiConversation.resumeRun`.
 */
public data class MonarchAiErrorPart(
    public val code: String,
    public val message: String,
    public val retryable: Boolean,
    public val retryAfterMs: Long? = null,
    public val resumable: Boolean = false,
) : MonarchAiPart {
    override val type: String get() = "error"
}
