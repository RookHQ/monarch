package com.rookhq.monarch.components.intelligence.aiusagemeter

/** What an AI usage scope counts. The same names exist on every platform. */
public enum class MonarchAiUsageUnit {
    /** Model tokens. The default. */
    Tokens,

    /** Billing credits. */
    Credits,
}
