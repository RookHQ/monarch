package com.rookhq.monarch.components.technical.logviewer

/** How the search of a `MonarchLogViewer` treats lines without a match. Both modes mark the matches. */
public enum class MonarchLogViewerSearchMode {
    /** Lines without a match are hidden. */
    Filter,

    /** Every line stays; the matches are marked. */
    Highlight,
}
