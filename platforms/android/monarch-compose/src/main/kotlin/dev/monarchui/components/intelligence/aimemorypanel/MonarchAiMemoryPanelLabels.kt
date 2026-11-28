package dev.monarchui.components.intelligence.aimemorypanel

import androidx.compose.runtime.Immutable
import dev.monarchui.components.datagrid.bulkactionbar.MonarchBulkActionBarLabels
import dev.monarchui.components.intelligence.aimemorychip.MonarchAiMemoryChipLabels

/**
 * Replacements for the strings `MonarchAiMemoryPanel` supplies. Each `null`
 * keeps the built-in English string.
 *
 * @property search the search field's placeholder and name. Default "Search memories".
 * @property selectAll the row that selects every memory shown. Default "Select all (3)".
 * @property selectGroup the name of a group's checkbox. Default "Select all memories about {name}".
 * @property select the name of a memory's checkbox. Default "Select memory: {text}".
 * @property kind the kind under a group's name. Default "Person", "Organization", "Project", "Other".
 * @property count the number of memories in a group. Default "2 memories".
 * @property updated the line under a memory, from the formatted date. Default "Updated {date}".
 * @property empty shown when nothing is remembered. Default "Nothing is remembered yet.".
 * @property noMatches shown when the search matches nothing. Default "No memories match “{query}”.".
 * @property matches announced after a search. Default "2 memories match".
 * @property forget the bulk action and the confirm button. Default "Forget".
 * @property confirmTitle the confirmation dialog's title. Default "Forget 2 memories?".
 * @property confirmMessage the confirmation dialog's text.
 * @property cancel the confirmation dialog's dismiss button. Default "Cancel".
 * @property forgotten announced after forgetting. Default "Memory forgotten", "2 memories forgotten".
 * @property edited announced after an edit. Default "Memory updated".
 * @property chip the strings of each memory row (edit, forget, the edit dialog).
 * @property bulkBar the strings of the bar shown while memories are selected.
 */
@Immutable
public class MonarchAiMemoryPanelLabels(
    public val search: String? = null,
    public val selectAll: ((shown: Int) -> String)? = null,
    public val selectGroup: ((name: String) -> String)? = null,
    public val select: ((text: String) -> String)? = null,
    public val kind: ((kind: MonarchAiMemoryEntityKind) -> String)? = null,
    public val count: ((count: Int) -> String)? = null,
    public val updated: ((date: String) -> String)? = null,
    public val empty: String? = null,
    public val noMatches: ((query: String) -> String)? = null,
    public val matches: ((count: Int) -> String)? = null,
    public val forget: String? = null,
    public val confirmTitle: ((count: Int) -> String)? = null,
    public val confirmMessage: String? = null,
    public val cancel: String? = null,
    public val forgotten: ((count: Int) -> String)? = null,
    public val edited: String? = null,
    public val chip: MonarchAiMemoryChipLabels = MonarchAiMemoryChipLabels(),
    public val bulkBar: MonarchBulkActionBarLabels = MonarchBulkActionBarLabels(),
) {
    private val values: List<Any?>
        get() = listOf(
            search, selectAll, selectGroup, select, kind, count, updated, empty, noMatches, matches, forget,
            confirmTitle, confirmMessage, cancel, forgotten, edited, chip, bulkBar,
        )

    override fun equals(other: Any?): Boolean = other is MonarchAiMemoryPanelLabels && other.values == values

    override fun hashCode(): Int = values.hashCode()
}
