package dev.monarchui.components.intelligence.aicitation

import androidx.compose.runtime.Immutable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind

/**
 * Replacements for the built-in strings of a `MonarchAiCitation`. Every `null` uses the built-in string named
 * after it in `strings_aicitation.xml`.
 *
 * @property marker the spoken name of the marker, from the number and the source's title: "Source 1: Pricing".
 * @property card the heading of the card and the name of the popover, from the number: "Source 1".
 * @property open the button that calls `onOpen`: "Open source".
 * @property visit the link to the source's address: "Visit page".
 * @property kind the name of a kind of source in the meta line: "Web page", "Document", "Record", "File".
 */
@Immutable
public class MonarchAiCitationLabels(
    public val marker: ((index: Int, title: String) -> String)? = null,
    public val card: ((index: Int) -> String)? = null,
    public val open: String? = null,
    public val visit: String? = null,
    public val kind: ((MonarchAiSourceKind) -> String)? = null,
)
