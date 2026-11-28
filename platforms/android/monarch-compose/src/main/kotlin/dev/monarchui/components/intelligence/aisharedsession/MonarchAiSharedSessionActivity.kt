package dev.monarchui.components.intelligence.aisharedsession

/** What a participant of a `MonarchAiSharedSession` is doing right now. */
public enum class MonarchAiSharedSessionActivity {
    /** Writing a message. */
    Typing,

    /** Talking in a voice session. */
    Speaking,

    /** Reading an answer or a proposal. */
    Reviewing,
}
