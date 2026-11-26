package com.rookhq.monarch.components.files.fileupload

import android.content.Context
import com.rookhq.monarch.internal.monarchFormatBytes

/**
 * The files a check accepted and the ones it refused, each in the order given.
 *
 * @property accepted the files that passed every check.
 * @property rejected the refused files, each with its reason and message.
 */
public class MonarchFileUploadValidation(
    public val accepted: List<MonarchPickedFile>,
    public val rejected: List<MonarchFileRejection>,
)

/**
 * The checks of `MonarchFileUpload`, for files that come from another source
 * than its picker: a paste, a share into the app, a drop. The same order and
 * messages as the component: the type, the size, the app's own check, then
 * the count.
 */
public object MonarchFileUploadValidator {
    /**
     * Splits [files] into accepted and refused.
     *
     * @param context resolves the built-in messages and writes the size limit in the user's locale.
     * @param files the files to check, in order.
     * @param accept allowed types: MIME types, wildcards such as `image/` with a star, and extensions such as
     * `.csv`. Empty allows every file.
     * @param maxSize the largest size per file, in bytes.
     * @param maxFiles the most files in total, counting [currentCount].
     * @param currentCount files already attached.
     * @param multiple whether several files may be added at once. Without it only the first file is kept.
     * @param labels replacements for the built-in messages.
     * @param validate the app's own check: return a message to refuse a file, or `null` to accept it.
     */
    @Suppress("LongParameterList")
    public fun validate(
        context: Context,
        files: List<MonarchPickedFile>,
        accept: List<String> = emptyList(),
        maxSize: Long? = null,
        maxFiles: Int? = null,
        currentCount: Int = 0,
        multiple: Boolean = maxFiles != 1,
        labels: MonarchFileUploadLabels = MonarchFileUploadLabels(),
        validate: ((MonarchPickedFile) -> String?)? = null,
    ): MonarchFileUploadValidation {
        val checks = MonarchFileUploadTokens.check(files, accept, maxSize, maxFiles, currentCount, multiple, validate)
        val formatSize: (Long) -> String = labels.formatSize ?: { monarchFormatBytes(it) }
        val messages = FileUploadMessages(context.resources, labels, formatSize)
        return MonarchFileUploadValidation(
            accepted = checks.filter { it.code == null }.map { it.file },
            rejected = messages.rejections(checks, maxSize, if (multiple) maxFiles else 1),
        )
    }
}
