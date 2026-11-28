package dev.monarchui.components.files.fileupload

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Immutable

/**
 * A file chosen with a system picker, as a `MonarchFileUpload` hands it over.
 *
 * The app reads the content from [uri] with `ContentResolver.openInputStream`
 * and uploads it. The read permission the picker grants lasts while the app's
 * task is alive; take a persistable permission to keep it longer.
 *
 * @property uri the content URI the picker returned.
 * @property name the display name, for example "invoice.pdf".
 * @property mimeType the MIME type the provider reports, or `null` when unknown.
 * @property size the size in bytes, or `null` when the provider does not know it.
 */
@Immutable
public class MonarchPickedFile(
    public val uri: Uri,
    public val name: String,
    public val mimeType: String?,
    public val size: Long?,
) {
    /** Reads the name, MIME type and size of a content URI. */
    public companion object {
        /**
         * Describes [uri] with the provider's `OpenableColumns` and type. Use it
         * for files that reach the app another way, for example a share intent.
         *
         * @param resolver the content resolver, from `Context.contentResolver`.
         * @param uri a content URI with read permission.
         */
        public fun of(resolver: ContentResolver, uri: Uri): MonarchPickedFile {
            var name: String? = null
            var size: Long? = null
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameColumn >= 0 && !cursor.isNull(nameColumn)) name = cursor.getString(nameColumn)
                        if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) size = cursor.getLong(sizeColumn)
                    }
                }
            return MonarchPickedFile(uri, name ?: uri.lastPathSegment.orEmpty(), resolver.getType(uri), size)
        }
    }
}
