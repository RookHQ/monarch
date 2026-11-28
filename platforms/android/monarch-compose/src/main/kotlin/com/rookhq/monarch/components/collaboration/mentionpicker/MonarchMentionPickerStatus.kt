package com.rookhq.monarch.components.collaboration.mentionpicker

/** The state of the search behind a `MonarchMentionPicker`. */
public enum class MonarchMentionPickerStatus {
    /** The search runs: placeholder rows, read as "Searching…". */
    Loading,

    /** The search finished: the suggestions, or "No matches". The default. */
    Ready,

    /** The search failed: an error with Retry when `onRetry` is given. */
    Failed,
}
