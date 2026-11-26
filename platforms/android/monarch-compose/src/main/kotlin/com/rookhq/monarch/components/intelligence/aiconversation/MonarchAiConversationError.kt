package com.rookhq.monarch.components.intelligence.aiconversation

/**
 * Why the last turn of a conversation failed.
 *
 * @property code the error code, for example `network`, `rate-limited` or `http-503`. Never show it;
 * `MonarchAiErrorKind.fromCode` maps it to a kind with plain words.
 * @property message the message for the person.
 * @property retryable whether the turn may be tried again unchanged.
 * @property retryAfterMs milliseconds until a retry is allowed, when the service said.
 * @property resumable whether the connection dropped mid-reply and the run can be picked up with
 * `resumeRun` rather than asked again.
 */
public data class MonarchAiConversationError(
    public val code: String,
    public val message: String,
    public val retryable: Boolean,
    public val retryAfterMs: Long? = null,
    public val resumable: Boolean = false,
) {
    /** Decides whether a code may be retried. */
    public companion object {
        private const val HTTP_PREFIX = "http-"
        private const val STATUS_DIGITS = 3
        private const val FIRST_SERVER_STATUS = 500
        private val RetryableCodes = setOf(
            "network", "timeout", "rate-limited", "rate-limit", "server", "overloaded", "overloaded_error",
            "rate_limit_error", "api_error",
        )

        /**
         * Whether a failed turn with this code may be tried again unchanged:
         * network drops, timeouts, rate limits, overload and server errors
         * (`http-5xx`). Unknown codes are not retryable, so a broken request is
         * not repeated.
         */
        public fun isRetryable(code: String?): Boolean {
            if (code.isNullOrEmpty()) return false
            if (code in RetryableCodes) return true
            val status = code.removePrefix(HTTP_PREFIX).takeIf {
                code.startsWith(HTTP_PREFIX) && it.length == STATUS_DIGITS
            }?.toIntOrNull()
            return status != null && status >= FIRST_SERVER_STATUS
        }
    }
}
