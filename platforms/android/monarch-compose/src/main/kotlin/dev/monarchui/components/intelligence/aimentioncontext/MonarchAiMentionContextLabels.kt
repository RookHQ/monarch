package dev.monarchui.components.intelligence.aimentioncontext

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiMentionContext`.
 * Every `null` keeps the built-in string `monarch_aimentioncontext_<key>`.
 *
 * @property label the name of the list, "Add context".
 * @property group the heading of each kind, shown when results have more than one kind: "Documents", "People",
 * "Records", "Apps".
 * @property loading shown and read while searching, "Searching…".
 * @property empty shown and read when nothing matches, given the query: "Nothing matches “q3”", or "Type to
 * search" for an empty query.
 * @property error shown and read when the search failed.
 * @property retry the button that repeats a failed search.
 * @property tokens a result's token estimate, given the compact number and the count: "12.4K tokens".
 * @property count read when results arrive, given the number: "3 results".
 */
@Immutable
public class MonarchAiMentionContextLabels(
    public val label: String? = null,
    public val group: ((MonarchAiMentionKind) -> String)? = null,
    public val loading: String? = null,
    public val empty: ((query: String) -> String)? = null,
    public val error: String? = null,
    public val retry: String? = null,
    public val tokens: ((formatted: String, count: Int) -> String)? = null,
    public val count: ((Int) -> String)? = null,
)
