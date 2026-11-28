package dev.monarchui.components.intelligence.aigeneratedbadge

/** How much of the content came from AI. The same three names exist on every platform. */
public enum class MonarchAiGeneratedBadgeKind {
    /** Written by AI. The default. */
    Generated,

    /** Written by a person with AI help. */
    Assisted,

    /** Written by AI and changed by a person since. Drawn with a dashed border. */
    Edited,
}
