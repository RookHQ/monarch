package dev.monarchui.components.search.searchresults

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * A labelled group of results in `MonarchSearchResults`, such as
 * "Documents" or "People".
 *
 * Two groups are equal when their [id]s are equal.
 *
 * @property title the group's heading. `null` for results without groups: no heading is drawn.
 * @property id a stable id, unique among the groups.
 * @property results the results shown in this group. A group without results is left out.
 * @property total how many results the group has in all, when more exist than are shown. `null` is the
 * number shown; a total smaller than the number shown is raised to it.
 * @property icon an icon before the heading. Decorative.
 */
@Immutable
public class MonarchSearchResultGroup(
    public val title: String?,
    public val id: String,
    public val results: List<MonarchSearchResult>,
    public val total: Int? = null,
    public val icon: (@Composable () -> Unit)? = null,
) {
    override fun equals(other: Any?): Boolean = other is MonarchSearchResultGroup && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "MonarchSearchResultGroup(id=$id, title=$title, results=${results.size})"
}
