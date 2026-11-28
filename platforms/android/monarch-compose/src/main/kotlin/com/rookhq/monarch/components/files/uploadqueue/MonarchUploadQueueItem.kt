package com.rookhq.monarch.components.files.uploadqueue

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.files.attachmentchip.MonarchUploadStatus

/**
 * One upload of a `MonarchUploadQueue`, as the app reports it. The app does
 * the transfer and replaces the item when its status or progress changes.
 *
 * @property id a stable, unique id, passed back to the callbacks.
 * @property name the file name, shown and used in the TalkBack names of the buttons.
 * @property status where the upload is.
 * @property size the size in bytes, shown and used to weight the total progress; `null` when unknown.
 * @property contentType the MIME type, which picks the file type icon with the name.
 * @property progress the transferred fraction from 0 to 1 while uploading; `null` shows a moving bar.
 * @property errorMessage why the upload failed, shown after "Upload failed".
 * @property retryable whether Retry is offered once the upload failed or was cancelled.
 * @property detail a status line that replaces the built-in one, for example "Checking for viruses".
 */
@Immutable
public data class MonarchUploadQueueItem(
    public val id: String,
    public val name: String,
    public val status: MonarchUploadStatus,
    public val size: Long? = null,
    public val contentType: String? = null,
    public val progress: Float? = null,
    public val errorMessage: String? = null,
    public val retryable: Boolean = true,
    public val detail: String? = null,
)
