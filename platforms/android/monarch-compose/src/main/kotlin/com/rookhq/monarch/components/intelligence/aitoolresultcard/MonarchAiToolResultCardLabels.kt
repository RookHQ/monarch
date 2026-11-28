package com.rookhq.monarch.components.intelligence.aitoolresultcard

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiToolResultCard`.
 * Every `null` keeps the built-in string `monarch_aitoolresultcard_<key>`.
 *
 * @property kind the label above the title for a kind, such as "Record created". Return `null` to keep the
 * built-in label of that kind.
 * @property open the visible text of the open link, "Open".
 * @property openLabel the TalkBack name of the open link, from the title: "Open Acme Ltd".
 * @property undo the title of the undo button, "Undo".
 * @property undone shown and announced after a successful undo, "Undone".
 * @property undoFailed shown and announced when the undo failed, "Couldn’t undo. Try again."
 */
@Immutable
public class MonarchAiToolResultCardLabels(
    public val kind: ((MonarchAiToolResultKind) -> String?)? = null,
    public val open: String? = null,
    public val openLabel: ((title: String) -> String)? = null,
    public val undo: String? = null,
    public val undone: String? = null,
    public val undoFailed: String? = null,
)
