package com.rookhq.monarch.components.intelligence.aieditproposal

/** A decision announced by a `MonarchAiEditProposal`. */
public enum class MonarchAiEditProposalEvent {
    /** One change was accepted. */
    Accepted,

    /** One change was rejected. */
    Rejected,

    /** Every undecided change was accepted. */
    AcceptedAll,

    /** Every undecided change was rejected. */
    RejectedAll,
}
