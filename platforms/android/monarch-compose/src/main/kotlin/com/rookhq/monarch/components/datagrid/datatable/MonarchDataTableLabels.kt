package com.rookhq.monarch.components.datagrid.datatable

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchDataTable`. Every `null`
 * uses the built-in string named after it in `strings_datatable.xml`.
 *
 * @property selectAll the select-all checkbox: "Select all rows".
 * @property selectRow a row's checkbox from the row's name: "Select Ticket 42".
 * @property selectedCount beside the select-all checkbox from the number selected: "3 selected".
 * @property rowActions the name of a row's actions button from the row's name: "Actions for Ticket 42".
 * @property actionsText the visible text of the actions button when no icon is given: "Actions".
 * @property open the name of a row's tap action with `onOpen`: "Open details".
 * @property sort the list layout's sort button: "Sort".
 * @property sortedAscending the state of a header or the sort button sorted ascending: "Sorted ascending".
 * @property sortedDescending the state of a header or the sort button sorted descending: "Sorted descending".
 * @property notSorted the state of a sortable header that is not sorted: "Not sorted".
 * @property noValue read for an empty cell: "No value".
 * @property loading the name of the loading rows: "Loading rows".
 * @property emptyTitle the title of the empty state: "Nothing here yet".
 * @property emptyMessage the message of the empty state: "Rows appear here once they are added.".
 * @property filteredTitle the title of the filtered empty state: "No rows match these filters".
 * @property filteredMessage the message of the filtered empty state: "Try removing a filter or searching for
 * something else.".
 * @property errorTitle the title of the error state: "These rows could not be loaded".
 * @property ascendingMark the visible mark after a header sorted ascending, "↑". Not read by TalkBack, which
 * reads [sortedAscending].
 * @property descendingMark the visible mark after a header sorted descending, "↓".
 * @property emptyCell the visible text of an empty cell, "—". TalkBack reads [noValue].
 */
@Immutable
public class MonarchDataTableLabels(
    public val selectAll: String? = null,
    public val selectRow: ((row: String) -> String)? = null,
    public val selectedCount: ((count: Int) -> String)? = null,
    public val rowActions: ((row: String) -> String)? = null,
    public val actionsText: String? = null,
    public val open: String? = null,
    public val sort: String? = null,
    public val sortedAscending: String? = null,
    public val sortedDescending: String? = null,
    public val notSorted: String? = null,
    public val noValue: String? = null,
    public val loading: String? = null,
    public val emptyTitle: String? = null,
    public val emptyMessage: String? = null,
    public val filteredTitle: String? = null,
    public val filteredMessage: String? = null,
    public val errorTitle: String? = null,
    public val ascendingMark: String? = null,
    public val descendingMark: String? = null,
    public val emptyCell: String? = null,
)
