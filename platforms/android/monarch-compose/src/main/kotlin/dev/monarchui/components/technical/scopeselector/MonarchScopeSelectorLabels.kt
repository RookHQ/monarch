package dev.monarchui.components.technical.scopeselector

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchScopeSelector`. Every `null` keeps the built-in string
 * `monarch_scopeselector_<key>`.
 *
 * @property label the heading and TalkBack name of the selector, "Scopes".
 * @property empty shown when there are no groups.
 * @property loading the summary of a group while its scopes load.
 * @property noScopes the summary of a group without scopes.
 * @property count the summary of a group, from the number selected and the number of scopes: "2 of 5 scopes".
 * @property selectAll the Select all button.
 * @property clearAll the Clear all button.
 * @property selectAllIn the TalkBack name of Select all, from the group's name.
 * @property clearAllIn the TalkBack name of Clear all, from the group's name.
 * @property showMore the paging button, from how many it shows and how many are hidden.
 */
@Immutable
public class MonarchScopeSelectorLabels(
    public val label: String? = null,
    public val empty: String? = null,
    public val loading: String? = null,
    public val noScopes: String? = null,
    public val count: ((selected: Int, total: Int) -> String)? = null,
    public val selectAll: String? = null,
    public val clearAll: String? = null,
    public val selectAllIn: ((group: String) -> String)? = null,
    public val clearAllIn: ((group: String) -> String)? = null,
    public val showMore: ((next: Int, remaining: Int) -> String)? = null,
)
