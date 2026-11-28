package com.rookhq.monarch.components.intelligence.aiconversation

/** How much harm running a tool can do. `Destructive` cannot be undone. */
public enum class MonarchAiToolCallRisk {
    /** Little harm. */
    Low,

    /** Some harm. */
    Medium,

    /** Much harm. */
    High,

    /** Cannot be undone. */
    Destructive,
}
