package dev.monarchui.components.intelligence.aiconversation

/** What happened to a proposed edit. */
public enum class MonarchAiEditProposalState {
    /** Waiting for a person. */
    Proposed,

    /** Applied to the target. */
    Applied,

    /** Rejected. */
    Rejected,

    /** Applied and then undone. */
    Reverted,
}
