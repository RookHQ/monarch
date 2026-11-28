package dev.monarchui.components.files.embedblock

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchEmbedBlock`. Every
 * `null` uses the built-in string named after it in `strings_embedblock.xml`.
 *
 * @property frameTitle writes the TalkBack name of the frame from the provider's name when there is no caption
 * or title: "YouTube content".
 * @property invalid the message for a URL that cannot be shown: "This link can't be shown.".
 */
@Immutable
public class MonarchEmbedBlockLabels(
    public val frameTitle: ((provider: String) -> String)? = null,
    public val invalid: String? = null,
)
