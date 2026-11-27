package com.rookhq.monarch.components.files.filecard

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchFileCard`. Every `null`
 * uses the built-in string named after it in `strings_filecard.xml`.
 *
 * @property select writes the TalkBack name of the selection checkbox from the file name: "Select Q3 report.pdf".
 * @property type replaces the type at the start of the meta line, "PDF document" (the spoken name of the
 * file type icon).
 */
@Immutable
public class MonarchFileCardLabels(
    public val select: ((name: String) -> String)? = null,
    public val type: String? = null,
)
