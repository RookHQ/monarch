package dev.monarchui.components.feedback.savestatusindicator

/** Where saving stands, for a `MonarchSaveStatusIndicator`. The same names exist on every platform. */
public enum class MonarchSaveStatus {
    /** Nothing to report. Nothing is drawn. */
    Idle,

    /** Edits are not saved yet. */
    Unsaved,

    /** A save is in progress. */
    Saving,

    /** The last save finished. Announced. */
    Saved,

    /** The last save failed. Announced. */
    Error,
}
