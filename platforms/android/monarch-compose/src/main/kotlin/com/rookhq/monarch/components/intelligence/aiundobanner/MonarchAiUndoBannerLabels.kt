package com.rookhq.monarch.components.intelligence.aiundobanner

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiUndoBanner`.
 * Every `null` keeps the built-in string `monarch_aiundobanner_<key>`.
 *
 * @property aiPrefix read before the title so the banner is identified as an AI change, "AI change:".
 * @property undo the title of the undo button, "Undo".
 * @property audit the text of the audit log link, "View in audit log".
 * @property dismiss the title of the dismiss button, "Dismiss".
 * @property showChanges the title of the disclosure that holds more than three changes, from their count:
 * "Show 5 changes".
 * @property change the spoken form of one change, from the label, the old and the new value:
 * "Status changed from Open to Closed".
 * @property undone shown and announced after a successful undo, "Changes undone".
 * @property undoFailed shown and announced when the undo failed, "Couldn’t undo. Try again."
 * @property expired shown and announced when the time to undo has run out, "Undo is no longer available."
 */
@Immutable
public class MonarchAiUndoBannerLabels(
    public val aiPrefix: String? = null,
    public val undo: String? = null,
    public val audit: String? = null,
    public val dismiss: String? = null,
    public val showChanges: ((count: Int) -> String)? = null,
    public val change: ((label: String, before: String, after: String) -> String)? = null,
    public val undone: String? = null,
    public val undoFailed: String? = null,
    public val expired: String? = null,
)
