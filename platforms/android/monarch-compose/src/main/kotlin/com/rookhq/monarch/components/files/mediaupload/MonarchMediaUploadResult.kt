package com.rookhq.monarch.components.files.mediaupload

import androidx.compose.runtime.Immutable

/**
 * What `onUpload` of a `MonarchMediaUpload` returns.
 *
 * @property url the address of the stored file, passed to `onValueChange`.
 * @property warnings messages to show under the field after the upload, such as "The image was resized.".
 */
@Immutable
public class MonarchMediaUploadResult(
    public val url: String,
    public val warnings: List<String> = emptyList(),
)
