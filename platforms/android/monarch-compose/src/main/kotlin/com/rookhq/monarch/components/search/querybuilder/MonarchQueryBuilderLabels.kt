package com.rookhq.monarch.components.search.querybuilder

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchQueryBuilder`. Every `null` keeps the built-in string
 * `monarch_querybuilder_<key>`.
 *
 * @property label the TalkBack name of the builder, "Query".
 * @property group the name of a group from its path: `[1]` is the top group ("Top group"), `[1, 2]` its second
 * sub-group ("Group 2"), `[1, 2, 1]` that group's first ("Group 2.1").
 * @property combinatorBefore the text before a group's AND / OR switch, "Join rules with".
 * @property and the AND segment.
 * @property or the OR segment.
 * @property combinatorLabel the TalkBack name of a group's switch, from the group's name.
 * @property rule the heading of a rule, from its position in its group: "Rule 1".
 * @property removeRule the TalkBack name of a rule's remove button, from the group's name and the position.
 * @property addRule the button that adds a rule.
 * @property addGroup the button that adds a sub-group.
 * @property removeGroup the TalkBack name of a group's remove button, from the group's name.
 * @property remove the visible text of a remove button without an icon, "Remove".
 * @property emptyGroup shown in a group with no rules.
 * @property maxDepthReached shown instead of Add group at the deepest level.
 */
@Immutable
public class MonarchQueryBuilderLabels(
    public val label: String? = null,
    public val group: ((path: List<Int>) -> String)? = null,
    public val combinatorBefore: String? = null,
    public val and: String? = null,
    public val or: String? = null,
    public val combinatorLabel: ((group: String) -> String)? = null,
    public val rule: ((position: Int) -> String)? = null,
    public val removeRule: ((group: String, position: Int) -> String)? = null,
    public val addRule: String? = null,
    public val addGroup: String? = null,
    public val removeGroup: ((group: String) -> String)? = null,
    public val remove: String? = null,
    public val emptyGroup: String? = null,
    public val maxDepthReached: String? = null,
)
