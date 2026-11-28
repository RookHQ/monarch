package dev.monarchui.components.datagrid.columnchooser

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchColumnChooser`. Every
 * `null` uses the built-in string named after it in `strings_columnchooser.xml`.
 *
 * @property title the heading: "Columns".
 * @property shownCount the count under the heading from the shown and all columns: "5 of 8 shown".
 * @property search the placeholder and name of the search field: "Find a column".
 * @property noMatches shown when the search matches nothing: "No columns match.".
 * @property required the note under a column that cannot be hidden: "Always shown".
 * @property showAll the button that shows every column: "Show all".
 * @property reset the button that restores the app's default columns: "Reset".
 * @property clearSearch the name of the search field's clear button: "Clear search".
 * @property handle the name of a row's drag handle, which carries the Move up and Move down actions:
 * "Reorder Status". `null` uses `MonarchReorderableList`'s string.
 * @property moved announced after a move by an action: "Status moved to position 3 of 8.". `null` uses
 * `MonarchReorderableList`'s string.
 */
@Immutable
public class MonarchColumnChooserLabels(
    public val title: String? = null,
    public val shownCount: ((shown: Int, total: Int) -> String)? = null,
    public val search: String? = null,
    public val noMatches: String? = null,
    public val required: String? = null,
    public val showAll: String? = null,
    public val reset: String? = null,
    public val clearSearch: String? = null,
    public val handle: ((label: String) -> String)? = null,
    public val moved: ((label: String, position: Int, total: Int) -> String)? = null,
)
