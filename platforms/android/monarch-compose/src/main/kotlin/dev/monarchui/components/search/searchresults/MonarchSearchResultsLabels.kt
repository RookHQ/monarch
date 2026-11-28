package dev.monarchui.components.search.searchresults

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of `MonarchSearchResults`. Every
 * `null` uses the built-in string in `strings_searchresults.xml`.
 *
 * @property summary writes the summary from the number of results and the trimmed query (empty when there is
 * none): "12 results for “budget”".
 * @property loading the summary while a search runs: "Searching…".
 * @property emptyTitle writes the empty state's title from the trimmed query: "No results for “budget”", or
 * "No results" without a query.
 * @property emptyMessage the empty state's message: "Try other words, check the spelling, or remove a filter."
 * @property errorTitle the error state's title: "Search failed".
 * @property retry the error state's button: "Try again".
 * @property showAll writes the button after a group with more results from the group's title and total:
 * "Show all 24 in Documents".
 * @property groupCount writes the visible count next to a group's heading: "24".
 * @property groupName writes what TalkBack reads for a group's heading from its title and total:
 * "Documents, 24 results".
 */
@Immutable
public class MonarchSearchResultsLabels(
    public val summary: ((count: Int, query: String) -> String)? = null,
    public val loading: String? = null,
    public val emptyTitle: ((query: String) -> String)? = null,
    public val emptyMessage: String? = null,
    public val errorTitle: String? = null,
    public val retry: String? = null,
    public val showAll: ((group: String, total: Int) -> String)? = null,
    public val groupCount: ((total: Int) -> String)? = null,
    public val groupName: ((group: String, total: Int) -> String)? = null,
)
