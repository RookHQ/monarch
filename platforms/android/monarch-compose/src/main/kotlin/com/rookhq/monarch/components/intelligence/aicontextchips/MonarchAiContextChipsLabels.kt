package com.rookhq.monarch.components.intelligence.aicontextchips

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiContextChips`. Every
 * `null` keeps the built-in string `monarch_aicontextchips_<key>`.
 *
 * @property label the name TalkBack reads for the group of chips, "Context".
 * @property kind the name of a kind, read before the item's name: "File".
 * @property tokens the token count of one item, from the compact count and the exact count: "1.2K tokens".
 * @property total the total after the chips, from the compact count and the exact count:
 * "≈ 3.4K tokens in context".
 * @property remove the TalkBack action of a removable chip, from the item's name:
 * "Remove contract.pdf from context".
 * @property removed announced after a chip is removed, from the item's name: "contract.pdf removed from context".
 */
@Immutable
public class MonarchAiContextChipsLabels(
    public val label: String? = null,
    public val kind: ((MonarchAiContextKind) -> String)? = null,
    public val tokens: ((formatted: String, count: Int) -> String)? = null,
    public val total: ((formatted: String, count: Int) -> String)? = null,
    public val remove: ((label: String) -> String)? = null,
    public val removed: ((label: String) -> String)? = null,
)
