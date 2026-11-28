package dev.monarchui.components.files.uploadqueue

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchUploadQueue`. Every
 * `null` keeps the built-in string `monarch_uploadqueue_<key>`.
 *
 * @property title the heading ("Uploads").
 * @property queued the status of a queued upload ("Waiting").
 * @property uploading the status of an upload in progress, given the percentage or `null` ("Uploading… 40%").
 * @property done the status of a finished upload ("Uploaded").
 * @property failed the status of a failed upload, given its error message or `null` ("Upload failed: Too large").
 * @property cancelled the status of a cancelled upload ("Cancelled").
 * @property retry the visible text of the retry button ("Retry").
 * @property retryLabel the TalkBack name of the retry button, given the file name ("Retry upload of a.pdf").
 * @property cancel the visible text of the cancel button ("Cancel").
 * @property cancelLabel the TalkBack name of the cancel button, given the file name ("Cancel upload of a.pdf").
 * @property remove the visible text of the remove button ("Remove").
 * @property removeLabel the TalkBack name of the remove button, given the file name ("Remove a.pdf from the list").
 * @property clearFinished the header button that removes every upload that ended ("Clear finished").
 * @property progressLabel the TalkBack name of a file's progress bar, given the file name.
 * @property totalProgress the TalkBack name of the total progress bar ("Total upload progress").
 * @property summary the line under the heading, given the counts ("2 of 5 uploaded · 1 failed").
 * @property announceDone announced when an upload finishes, given the file name ("a.pdf uploaded.").
 * @property announceFailed announced when an upload fails, given the file name and the error or `null`.
 * @property announceFinished announced when the last active upload of several ends, given the counts.
 * @property formatSize formats a size in bytes. The default is the locale's short file size ("2.4 MB").
 */
@Immutable
public class MonarchUploadQueueLabels(
    public val title: String? = null,
    public val queued: String? = null,
    public val uploading: ((Int?) -> String)? = null,
    public val done: String? = null,
    public val failed: ((String?) -> String)? = null,
    public val cancelled: String? = null,
    public val retry: String? = null,
    public val retryLabel: ((String) -> String)? = null,
    public val cancel: String? = null,
    public val cancelLabel: ((String) -> String)? = null,
    public val remove: String? = null,
    public val removeLabel: ((String) -> String)? = null,
    public val clearFinished: String? = null,
    public val progressLabel: ((String) -> String)? = null,
    public val totalProgress: String? = null,
    public val summary: ((MonarchUploadQueueCounts) -> String)? = null,
    public val announceDone: ((String) -> String)? = null,
    public val announceFailed: ((String, String?) -> String)? = null,
    public val announceFinished: ((MonarchUploadQueueCounts) -> String)? = null,
    public val formatSize: ((Long) -> String)? = null,
)
