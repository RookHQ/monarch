package com.rookhq.monarch.components.files.attachmentchip

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAttachmentChip`. Every
 * `null` uses the built-in string named after it in `strings_attachmentchip.xml`.
 *
 * @property remove writes the spoken name of the remove button from the file name: "Remove report.pdf".
 * @property retry writes the spoken name of the retry button from the file name: "Retry report.pdf".
 * @property uploading writes the spoken name of the progress bar from the file name: "Uploading report.pdf".
 * @property queued shown while waiting to start: "Waiting".
 * @property failed shown when the upload failed and there is no error message: "Upload failed".
 * @property cancelled shown when the upload was cancelled: "Cancelled".
 * @property removeText the visible word of the remove button when there is no remove icon: "Remove".
 * @property retryText the visible word of the retry button when there is no retry icon: "Retry".
 */
@Immutable
public class MonarchAttachmentChipLabels(
    public val remove: ((name: String) -> String)? = null,
    public val retry: ((name: String) -> String)? = null,
    public val uploading: ((name: String) -> String)? = null,
    public val queued: String? = null,
    public val failed: String? = null,
    public val cancelled: String? = null,
    public val removeText: String? = null,
    public val retryText: String? = null,
)
