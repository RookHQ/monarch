package dev.monarchui.components.intelligence.aicodeblock

import androidx.compose.runtime.Immutable
import dev.monarchui.components.typography.codeblock.MonarchCodeBlockLabels

/**
 * Replacements for the built-in strings of a `MonarchAiCodeBlock`. Every
 * `null` keeps the built-in string `monarch_aicodeblock_<key>`.
 *
 * @property apply the Apply button, "Apply".
 * @property insert the Insert button, "Insert".
 * @property actionLabel the spoken name of Apply or Insert, given the button's text and the name of the code:
 * "Apply server.ts".
 * @property incompleteHint shown under the actions while the code is still arriving,
 * "Available when the code is complete".
 * @property codeBlock the strings of the `MonarchCodeBlock` inside: copy, wrap, diff words and the code's name.
 */
@Immutable
public class MonarchAiCodeBlockLabels(
    public val apply: String? = null,
    public val insert: String? = null,
    public val actionLabel: ((action: String, name: String) -> String)? = null,
    public val incompleteHint: String? = null,
    public val codeBlock: MonarchCodeBlockLabels = MonarchCodeBlockLabels(),
)
