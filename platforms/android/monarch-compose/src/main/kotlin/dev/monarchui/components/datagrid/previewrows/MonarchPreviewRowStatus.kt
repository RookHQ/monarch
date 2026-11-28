package dev.monarchui.components.datagrid.previewrows

/** What an import will do with one row of a `MonarchPreviewRows`. */
public enum class MonarchPreviewRowStatus {
    /** The row will be imported as it is. */
    Ready,

    /** The row will be imported, with warnings. */
    Warning,

    /** The row will be skipped because of errors. */
    Error,

    /** The row matches an existing record. */
    Duplicate,
}
