package com.rookhq.monarch.components.intelligence.aiattachmenttray

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiFilePart

/**
 * One file in a `MonarchAiAttachmentTray`.
 *
 * @property id a stable id, passed back to `onRemove` and `onRetry`.
 * @property name the file name.
 * @property sizeBytes the size in bytes, shown once uploaded.
 * @property contentType the MIME type; with the name it picks the type icon.
 * @property progress the uploaded fraction from 0 to 1 while uploading; `null` or 1 means uploaded.
 * @property failed whether the upload failed.
 * @property errorMessage why the upload failed. A message also marks the file as failed.
 */
@Immutable
public data class MonarchAiAttachmentTrayFile(
    public val id: String,
    public val name: String,
    public val sizeBytes: Long? = null,
    public val contentType: String? = null,
    public val progress: Float? = null,
    public val failed: Boolean = false,
    public val errorMessage: String? = null,
) {
    /**
     * A file of the conversation model, for a message that already carries its file parts.
     *
     * @param part the file part.
     * @param id the id; by default the part's file id, else its URL, else its name.
     * @param progress the uploaded fraction while uploading.
     */
    public constructor(part: MonarchAiFilePart, id: String? = null, progress: Float? = null) : this(
        id = id ?: part.fileId ?: part.url ?: part.name,
        name = part.name,
        sizeBytes = part.size,
        contentType = part.mediaType,
        progress = progress,
    )
}
