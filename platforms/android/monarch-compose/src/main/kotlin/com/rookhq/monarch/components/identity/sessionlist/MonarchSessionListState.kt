package com.rookhq.monarch.components.identity.sessionlist

/** The loading state of a `MonarchSessionList`. */
public enum class MonarchSessionListState {
    /** The sessions are loaded. An empty list shows the empty state. */
    Ready,

    /** The sessions are loading. */
    Loading,

    /** Loading failed. */
    Failed,
}
