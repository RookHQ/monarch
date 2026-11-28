package dev.monarchui.components.files.mediaupload

/**
 * Passed to `onUpload` of a `MonarchMediaUpload` with the file.
 *
 * Cancel upload cancels the coroutine `onUpload` runs in; stop the transfer when it is cancelled.
 *
 * @property kind the kind of the file being uploaded.
 * @param progress receives the fraction reported with [reportProgress].
 */
public class MonarchMediaUploadContext(
    public val kind: MonarchMediaUploadKind,
    private val progress: (Float) -> Unit,
) {
    /**
     * Reports how much is uploaded. Until the first report the progress bar is indeterminate.
     *
     * @param fraction the part uploaded, from 0 to 1. Values outside are clamped.
     */
    public fun reportProgress(fraction: Float) {
        progress(fraction.coerceIn(0f, 1f))
    }
}
