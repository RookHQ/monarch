package dev.monarchui.components.intelligence.aiconversation

/** Why the conversation asks the transport for a turn. */
public enum class MonarchAiTrigger {
    /** A new user message. */
    Send,

    /** The reply is asked for again. */
    Regenerate,

    /** A user message was edited and resent. */
    Edit,

    /** The reply continues after the client sent tool results back. */
    ToolResult,

    /** Reconnecting to a run that was already going. */
    Resume,
}
