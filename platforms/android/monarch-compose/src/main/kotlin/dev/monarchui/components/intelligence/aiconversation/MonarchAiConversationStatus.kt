package dev.monarchui.components.intelligence.aiconversation

/** What a `MonarchAiConversation` is doing. */
public enum class MonarchAiConversationStatus {
    /** Nothing is running. */
    Idle,

    /** Sent, waiting for the first event. */
    Submitted,

    /** Events are arriving. */
    Streaming,

    /** A tool call waits for the person to approve or deny it. */
    AwaitingApproval,

    /** The model called a tool the client runs: send the result with `submitToolResult`, or set `onToolCall`. */
    AwaitingToolResult,

    /** The last turn failed; `error` holds why. */
    Error,

    /** The person stopped the reply. */
    Stopped,
}
