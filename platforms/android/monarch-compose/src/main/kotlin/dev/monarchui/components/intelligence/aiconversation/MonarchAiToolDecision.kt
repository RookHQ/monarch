package dev.monarchui.components.intelligence.aiconversation

/** What a tool policy decides about a tool call before anyone is asked. */
public enum class MonarchAiToolDecision {
    /** Ask the person: the conversation waits in `AwaitingApproval`. */
    Ask,

    /** Approve without asking, for example for a tool the person chose to always allow. */
    Allow,

    /** Deny without asking. */
    Deny,
}
