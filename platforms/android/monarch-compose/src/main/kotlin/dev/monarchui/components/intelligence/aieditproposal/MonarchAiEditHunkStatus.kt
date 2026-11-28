package dev.monarchui.components.intelligence.aieditproposal

/** A person's decision on one change of a `MonarchAiEditProposal`. */
public enum class MonarchAiEditHunkStatus {
    /** Not decided yet. */
    Proposed,

    /** Accepted. */
    Accepted,

    /** Rejected. */
    Rejected,
}
