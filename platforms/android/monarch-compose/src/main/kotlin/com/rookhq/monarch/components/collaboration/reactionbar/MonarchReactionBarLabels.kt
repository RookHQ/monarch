package com.rookhq.monarch.components.collaboration.reactionbar

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchReactionBar`. Every
 * `null` uses the built-in string named after it in `strings_reactionbar.xml`.
 *
 * @property label the spoken name of the bar: "Reactions".
 * @property reaction the spoken name of a chip from the emoji's name and the count: "thumbs up, 3 reactions".
 * @property reactedBy read after a chip's name when the item has `reactedBy`: "Reacted by Ana and Ben".
 * @property add the spoken name of the add button: "Add reaction".
 * @property addText the visible text of the add button when no icon is given: "+".
 */
@Immutable
public class MonarchReactionBarLabels(
    public val label: String? = null,
    public val reaction: ((label: String, count: Int) -> String)? = null,
    public val reactedBy: ((names: String) -> String)? = null,
    public val add: String? = null,
    public val addText: String? = null,
)
