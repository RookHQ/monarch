package dev.monarchui.components.collaboration.messagerow

/** The delivery state of a `MonarchMessageRow`. */
public enum class MonarchMessageRowStatus {
    /** Delivered. */
    Sent,

    /** Still sending: the body is muted and "Sending…" is shown under it. */
    Pending,

    /** Sending failed: the error is shown with Retry and Discard. */
    Failed,
}
