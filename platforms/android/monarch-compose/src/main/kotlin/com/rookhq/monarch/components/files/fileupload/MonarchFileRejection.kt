package com.rookhq.monarch.components.files.fileupload

import androidx.compose.runtime.Immutable

/**
 * A file a `MonarchFileUpload` refused, with the reason and a message ready to show.
 *
 * @property file the refused file.
 * @property code why it was refused.
 * @property message the message shown under the picker, for example "photo.heic is not an allowed file type.".
 */
@Immutable
public class MonarchFileRejection(
    public val file: MonarchPickedFile,
    public val code: MonarchFileRejectionCode,
    public val message: String,
)
