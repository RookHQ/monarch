package dev.monarchui.components.intelligence.aisharedsession

/** How present a participant of a `MonarchAiSharedSession` is. */
public enum class MonarchAiSharedSessionPresence {
    /** Here and engaged. */
    Active,

    /** Here but quiet. */
    Idle,

    /** Stepped away. */
    Away,
}
