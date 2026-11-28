package dev.monarchui.components.files.filepreview

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchFilePreview`. Every
 * `null` uses the built-in string named after it in `strings_filepreview.xml`.
 *
 * @property previewOf writes the spoken name of the preview from the file name: "Preview of report.pdf".
 * @property unavailable shown when the type cannot be previewed: "There is no preview for this file type.".
 * @property failed shown when the preview failed to load: "The preview could not be loaded.".
 * @property download the label of the download button: "Download".
 * @property downloadName writes the spoken name of the download button from the file name: "Download report.pdf".
 * @property page writes the spoken name of a PDF page from its number and the page count: "Page 1 of 3".
 */
@Immutable
public class MonarchFilePreviewLabels(
    public val previewOf: ((name: String) -> String)? = null,
    public val unavailable: String? = null,
    public val failed: String? = null,
    public val download: String? = null,
    public val downloadName: ((name: String) -> String)? = null,
    public val page: ((number: Int, count: Int) -> String)? = null,
)
