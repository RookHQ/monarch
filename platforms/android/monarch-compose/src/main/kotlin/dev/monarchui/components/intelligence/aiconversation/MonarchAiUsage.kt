package dev.monarchui.components.intelligence.aiconversation

/**
 * Token counts for one turn, and its cost in millionths of the billing currency.
 *
 * @property inputTokens every token the model read, including any served from or written to a prompt cache.
 * @property outputTokens every token the model wrote, including reasoning tokens.
 * @property costMicros the cost in millionths of the billing currency.
 * @property cacheReadTokens how many of [inputTokens] were read from a prompt cache.
 * @property cacheWriteTokens how many of [inputTokens] were written to a prompt cache.
 * @property reasoningTokens how many of [outputTokens] were reasoning the person does not see.
 */
public data class MonarchAiUsage(
    public val inputTokens: Long,
    public val outputTokens: Long,
    public val costMicros: Long? = null,
    public val cacheReadTokens: Long? = null,
    public val cacheWriteTokens: Long? = null,
    public val reasoningTokens: Long? = null,
)
