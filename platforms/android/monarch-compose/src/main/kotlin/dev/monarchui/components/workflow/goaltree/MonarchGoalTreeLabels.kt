package dev.monarchui.components.workflow.goaltree

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchGoalTree`. Every `null`
 * keeps the built-in string `monarch_goaltree_<key>`.
 *
 * @property status the name of each status: "Not started", "On track", "At risk", "Off track", "Done".
 * @property expand the name of the button that opens a goal, given its title: "Expand Grow revenue".
 * @property collapse the name of the button that closes a goal, given its title: "Collapse Grow revenue".
 * @property alignedTo the alignment line, given the goal it supports: "Aligned to Customers come back".
 * @property addChild the text of the add button, "Add key result".
 * @property addChildTo what TalkBack reads for the add button, given the parent's title: "Add key result to …".
 * @property childCount the count shown on a closed goal: "3 key results".
 * @property empty the title of the empty state, "No goals yet".
 */
@Immutable
public class MonarchGoalTreeLabels(
    public val status: ((MonarchGoalStatus) -> String)? = null,
    public val expand: ((String) -> String)? = null,
    public val collapse: ((String) -> String)? = null,
    public val alignedTo: ((String) -> String)? = null,
    public val addChild: String? = null,
    public val addChildTo: ((String) -> String)? = null,
    public val childCount: ((Int) -> String)? = null,
    public val empty: String? = null,
)
