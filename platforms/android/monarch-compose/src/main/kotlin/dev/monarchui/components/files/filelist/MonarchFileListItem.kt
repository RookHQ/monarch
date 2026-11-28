package dev.monarchui.components.files.filelist

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One file of a `MonarchFileList`. The fields are those of `MonarchFileCard`.
 *
 * @property id a stable, unique id, used for the selection and as the lazy key.
 * @property name the file name with its extension.
 * @property sizeBytes the size in bytes, shown in the locale's file units.
 * @property contentType the MIME type, which with the name decides the type icon.
 * @property meta more text after the size, already localised, such as "Edited yesterday".
 * @property image a preview the app has loaded, or `null` for the type icon.
 * @property badges short states shown under the name, such as "Shared".
 * @property enabled whether the file can be opened and selected. A disabled file is left out of select all.
 */
@Immutable
public class MonarchFileListItem(
    public val id: String,
    public val name: String,
    public val sizeBytes: Long? = null,
    public val contentType: String? = null,
    public val meta: String? = null,
    public val image: Painter? = null,
    public val badges: List<String> = emptyList(),
    public val enabled: Boolean = true,
)
