package dev.monarchui.components.datagrid.datagrid

import androidx.compose.runtime.Immutable
import dev.monarchui.components.datagrid.editablecell.MonarchEditableCellLabels

/**
 * Replacements for the built-in strings of a `MonarchDataGrid`. Every `null` uses the built-in string named
 * after it in `strings_datagrid.xml`.
 *
 * @property selectAll the select-all checkbox: "Select all rows".
 * @property selectRow a row's checkbox from the row's name: "Select INV-00042".
 * @property selectedCount beside the select-all checkbox in the list layout, from the number selected: "3 selected".
 * @property rowActions the name of a row's actions button from the row's name: "Actions for INV-00042".
 * @property actionsText the visible text of the actions button when no icon is given: "Actions".
 * @property columnOptions the name of a header's tap action, which opens the column menu: "Column options".
 * @property sortAscending the column menu item: "Sort ascending".
 * @property sortDescending the column menu item: "Sort descending".
 * @property clearSort the column menu item of the sorted column: "Clear sort".
 * @property pinStart the column menu item: "Pin to start".
 * @property pinEnd the column menu item: "Pin to end".
 * @property unpin the column menu item of a pinned column: "Unpin".
 * @property hideColumn the column menu item: "Hide column".
 * @property widen the column menu item that makes the column wider: "Widen column".
 * @property narrow the column menu item that makes the column narrower: "Narrow column".
 * @property sortedAscending the state of a header or the sort button sorted ascending: "Sorted ascending".
 * @property sortedDescending the state of a header or the sort button sorted descending: "Sorted descending".
 * @property notSorted the state of a header or the sort button that is not sorted: "Not sorted".
 * @property pinnedStart the state of a header pinned to the start: "Pinned to start".
 * @property pinnedEnd the state of a header pinned to the end: "Pinned to end".
 * @property editLabel an editable cell's name from the column and the row: "Status for INV-00042".
 * @property cell a read-only cell as TalkBack reads it, from the column and the value: "Status: Paid".
 * @property open the name of the tap action that calls `onRowOpen`: "Open details".
 * @property editRecord the name of a record's tap action in the list layout, which opens the editor: "Edit".
 * @property sort the list layout's sort button: "Sort".
 * @property done the button that closes the record editor of the list layout: "Done".
 * @property noValue read for an empty cell: "No value".
 * @property loading the name of the loading indicator: "Loading rows".
 * @property emptyTitle the title of the empty state: "Nothing here yet".
 * @property emptyMessage the message of the empty state: "Rows appear here once they are added.".
 * @property editableCell replacements for the strings of the editable cells.
 */
@Immutable
public class MonarchDataGridLabels(
    public val selectAll: String? = null,
    public val selectRow: ((row: String) -> String)? = null,
    public val selectedCount: ((count: Int) -> String)? = null,
    public val rowActions: ((row: String) -> String)? = null,
    public val actionsText: String? = null,
    public val columnOptions: String? = null,
    public val sortAscending: String? = null,
    public val sortDescending: String? = null,
    public val clearSort: String? = null,
    public val pinStart: String? = null,
    public val pinEnd: String? = null,
    public val unpin: String? = null,
    public val hideColumn: String? = null,
    public val widen: String? = null,
    public val narrow: String? = null,
    public val sortedAscending: String? = null,
    public val sortedDescending: String? = null,
    public val notSorted: String? = null,
    public val pinnedStart: String? = null,
    public val pinnedEnd: String? = null,
    public val editLabel: ((column: String, row: String) -> String)? = null,
    public val cell: ((column: String, value: String) -> String)? = null,
    public val open: String? = null,
    public val editRecord: String? = null,
    public val sort: String? = null,
    public val done: String? = null,
    public val noValue: String? = null,
    public val loading: String? = null,
    public val emptyTitle: String? = null,
    public val emptyMessage: String? = null,
    public val editableCell: MonarchEditableCellLabels = MonarchEditableCellLabels(),
)
