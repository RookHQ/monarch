package dev.monarchui.components.intelligence.aiinlinediff

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiInlineDiff`. Every
 * `null` keeps the built-in string `monarch_aiinlinediff_<key>`.
 *
 * @property label the name of the whole diff; says the suggestions come from AI. "AI suggested changes".
 * @property deleted read before removed text, "Deleted:".
 * @property inserted read before added text, "Inserted:".
 * @property position the position of the active change from its number and the total: "Change 2 of 5".
 * @property none shown when there are no changes, "No suggested changes".
 * @property previous the button and action that move to the previous change, "Previous".
 * @property next the button and action that move to the next change, "Next".
 * @property accept the visible text of the accept button, "Accept".
 * @property reject the visible text of the reject button, "Reject".
 * @property acceptChange the spoken name of the accept button: "Accept change 2 of 5".
 * @property rejectChange the spoken name of the reject button: "Reject change 2 of 5".
 * @property announcement announced after a decision, from whether it was accepted and the changes left:
 * "Change accepted. 2 left."
 */
@Immutable
public class MonarchAiInlineDiffLabels(
    public val label: String? = null,
    public val deleted: String? = null,
    public val inserted: String? = null,
    public val position: ((index: Int, total: Int) -> String)? = null,
    public val none: String? = null,
    public val previous: String? = null,
    public val next: String? = null,
    public val accept: String? = null,
    public val reject: String? = null,
    public val acceptChange: ((index: Int, total: Int) -> String)? = null,
    public val rejectChange: ((index: Int, total: Int) -> String)? = null,
    public val announcement: ((accepted: Boolean, left: Int) -> String)? = null,
)
