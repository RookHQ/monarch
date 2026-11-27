package com.rookhq.monarch.components.files.fileupload

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchFileUpload`. Every
 * `null` uses the built-in string or plural named after it in
 * `strings_fileupload.xml`.
 *
 * @property label the main text of the zone: "Choose files to upload".
 * @property browse the button that opens the picker: "Browse files".
 * @property limits writes the hint from the limits, each `null` when not set: "Up to 10 MB each · 5 files max".
 * @property limitReached writes the hint once `maxFiles` files are attached: "Limit reached: 5 files attached.".
 * @property invalidType writes the refusal of a file of the wrong type from its name.
 * @property tooLarge writes the refusal of a file over `maxSize` from its name and the formatted limit.
 * @property tooMany writes the refusal of a file over `maxFiles` from its name and the limit.
 * @property added writes the announcement after files are accepted from their number: "2 files added.".
 * @property formatSize formats a size in bytes. By default the system's short file size.
 */
@Immutable
public class MonarchFileUploadLabels(
    public val label: String? = null,
    public val browse: String? = null,
    public val limits: ((maxSize: String?, maxFiles: Int?) -> String)? = null,
    public val limitReached: ((maxFiles: Int) -> String)? = null,
    public val invalidType: ((name: String) -> String)? = null,
    public val tooLarge: ((name: String, maxSize: String) -> String)? = null,
    public val tooMany: ((name: String, maxFiles: Int) -> String)? = null,
    public val added: ((count: Int) -> String)? = null,
    public val formatSize: ((bytes: Long) -> String)? = null,
)
