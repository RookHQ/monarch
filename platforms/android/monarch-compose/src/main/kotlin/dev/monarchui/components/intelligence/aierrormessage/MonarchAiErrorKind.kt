package dev.monarchui.components.intelligence.aierrormessage

/** Why an assistant turn failed. Each kind has its own title, description, tone and action order. */
public enum class MonarchAiErrorKind {
    /** Too many requests: `429`, `rate_limit_exceeded`, quota or overload. Warning tone. */
    RateLimit,

    /** The conversation is too long for the model: `context_length_exceeded`, `413`. Warning tone. */
    ContextOverflow,

    /** A safety filter blocked the request: `content_filter`, moderation. Warning tone. */
    SafetyBlock,

    /** The assistant could not be reached: timeouts, `502`, `503`, `504`. Danger tone. */
    Network,

    /** A tool the assistant used failed. Danger tone. */
    ToolFailure,

    /** Anything else. Danger tone. */
    Unknown,
    ;

    /** Maps raw error codes to kinds. */
    public companion object {
        private val Separators = Regex("[\\s_.:/]+")
        private val Rules: List<Pair<MonarchAiErrorKind, Regex>> = listOf(
            RateLimit to Regex("(^|-)(429|529)$|rate-?limit|too-many-requests|quota|overloaded|throttl"),
            ContextOverflow to Regex(
                "context-(length|overflow|window|limit)|(^|-)413$|token-limit|too-many-tokens|" +
                    "prompt-too-long|max-context",
            ),
            SafetyBlock to Regex("safety|content-(filter|policy)|moderation|policy|refusal|blocked"),
            ToolFailure to Regex("tool"),
            Network to Regex(
                "network|offline|timeout|timedout|timed-out|connection|econn|fetch-failed|unreachable|" +
                    "(^|-)(0|408|502|503|504)$",
            ),
        )

        /**
         * The kind of a raw error code: `429` and `rate_limit_exceeded` are
         * `RateLimit`, `context_length_exceeded` and `413` are
         * `ContextOverflow`, `content_filter` is `SafetyBlock`, `ETIMEDOUT` and
         * `503` are `Network`, `tool_error` is `ToolFailure`, anything else is
         * `Unknown`. Case, `_` and spaces are ignored, and `http-429`-style
         * codes are understood. The same rules as the web's `aiErrorKind`.
         */
        public fun fromCode(code: String?): MonarchAiErrorKind {
            if (code.isNullOrEmpty()) return Unknown
            val normalised = code.trim().lowercase().replace(Separators, "-")
            return Rules.firstOrNull { (_, rule) -> rule.containsMatchIn(normalised) }?.first ?: Unknown
        }
    }
}
