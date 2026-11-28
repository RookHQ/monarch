package dev.monarchui.components.files.attachmentchip

/** Where the upload of one file stands. The same names exist on every platform. */
public enum class MonarchUploadStatus {
    /** Waiting to start. */
    Queued,

    /** Being uploaded, with a progress bar. */
    Uploading,

    /** Uploaded. The default. */
    Done,

    /** The upload failed. Announced. */
    Failed,

    /** The upload was cancelled. */
    Cancelled,
    ;

    /** Whether the upload is still to finish: queued or uploading. */
    public val active: Boolean get() = this == Queued || this == Uploading
}
