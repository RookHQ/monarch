package com.rookhq.monarch.components.datadisplay.provenance

import androidx.compose.runtime.Immutable

/**
 * Replacements for the spoken words of a `MonarchProvenance`. Every `null`
 * uses the built-in string named after it in `strings_provenance.xml`.
 *
 * @property source writes the spoken source from the source text: "Source: Registry".
 * @property checked writes the spoken time from the relative time: "checked 2 hours ago".
 */
@Immutable
public class MonarchProvenanceLabels(
    public val source: ((source: String) -> String)? = null,
    public val checked: ((time: String) -> String)? = null,
)
