package dev.monarchui.components.collaboration.commentspanel

/** Which threads a `MonarchCommentsPanel` lists. */
public enum class MonarchCommentsPanelFilter {
    /** Threads that are still being discussed. */
    Open,

    /** Threads that were resolved. */
    Resolved,
}
