package com.rookhq.monarch.components.layout.sectioncard

/** What the body of a `MonarchSectionCard` shows. */
public enum class MonarchSectionCardState {
    /** The content. The default. */
    Ready,

    /** Three skeleton rows from `MonarchLoadingState`. */
    Loading,

    /** A small `MonarchEmptyState`: "Nothing here yet" and the empty message. */
    Empty,

    /** A small `MonarchErrorState`: "This section didn't load", the error message and a retry. */
    Error,
}
