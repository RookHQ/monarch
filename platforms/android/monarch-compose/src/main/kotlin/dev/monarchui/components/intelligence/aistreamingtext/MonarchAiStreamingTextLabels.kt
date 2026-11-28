package dev.monarchui.components.intelligence.aistreamingtext

import androidx.compose.runtime.Immutable

/**
 * Replacements for the spoken strings of a `MonarchAiStreamingText`. Every
 * `null` keeps the built-in string `monarch_aistreamingtext_<key>`.
 *
 * @property complete announced when the answer stops streaming, "Response complete".
 * @property codeBlock announced in place of a code block's contents, "Code block".
 * @property table announced in place of a table's contents, "Table".
 */
@Immutable
public class MonarchAiStreamingTextLabels(
    public val complete: String? = null,
    public val codeBlock: String? = null,
    public val table: String? = null,
)
