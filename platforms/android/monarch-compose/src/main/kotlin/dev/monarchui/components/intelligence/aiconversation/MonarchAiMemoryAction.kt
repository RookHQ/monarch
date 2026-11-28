package dev.monarchui.components.intelligence.aiconversation

/** What happened to a memory. */
public enum class MonarchAiMemoryAction {
    /** The assistant used a memory it had. */
    Read,

    /** The assistant saved or updated a memory. */
    Write,

    /** The assistant was told to forget a memory. */
    Forget,
}
