package com.rookhq.monarch.components.intelligence.aiagentcard

/** How much of an agent a `MonarchAiAgentCard` shows. */
public enum class MonarchAiAgentCardVariant {
    /** Every section that has content, and the actions. */
    Full,

    /** The avatar, name, role line, tool count, description and capability badges, for lists and pickers. */
    Compact,
}
