package com.rookhq.monarch.components.intelligence.aidraftqueue

/** Where a draft is in review, for `MonarchAiDraftQueue`. The same names exist on every platform. */
public enum class MonarchAiDraftStatus {
    /** Waiting for a decision. */
    Draft,

    /** A new version is being written. */
    Regenerating,

    /** Approved, and waiting to be sent when the queue has a send action. */
    Approved,

    /** Sent. */
    Sent,

    /** Skipped without sending. */
    Skipped,
}
