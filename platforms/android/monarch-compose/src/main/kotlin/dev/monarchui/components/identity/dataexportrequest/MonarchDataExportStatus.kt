package dev.monarchui.components.identity.dataexportrequest

/** Where a data export is, as the product reports it. */
public enum class MonarchDataExportStatus {
    /** Nothing requested yet: the request form is shown. */
    Idle,

    /** The request was received and is queued. */
    Requested,

    /** The file is being built. */
    Preparing,

    /** The file can be downloaded until it expires. */
    Ready,

    /** The download is no longer available; the person can request again. */
    Expired,

    /** The export could not be built; the person can request again. */
    Failed,
}
