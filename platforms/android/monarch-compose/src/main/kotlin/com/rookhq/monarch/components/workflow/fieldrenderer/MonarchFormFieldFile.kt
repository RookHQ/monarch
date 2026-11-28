package com.rookhq.monarch.components.workflow.fieldrenderer

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.files.fileupload.MonarchPickedFile

/**
 * A file answer. The app uploads [file] and may replace the answer with a stored [name] and no file.
 *
 * @property name the file name shown in the list.
 * @property size the size in bytes, when known.
 * @property file the picked file, with its content URI, until the app has uploaded it.
 */
@Immutable
public data class MonarchFormFieldFile(
    public val name: String,
    public val size: Long? = null,
    public val file: MonarchPickedFile? = null,
)
