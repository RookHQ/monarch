package com.rookhq.monarch.components.datagrid.datatable

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rookhq.monarch.R

/**
 * The built-in strings of a `MonarchDataTable`, with the caller's replacements applied.
 *
 * @param labels the caller's replacements.
 * @param string the built-in string of a resource id, read in composition.
 */
internal class MonarchDataTableTexts(labels: MonarchDataTableLabels, string: (Int) -> String) {
    val selectAll: String = labels.selectAll ?: string(R.string.monarch_datatable_select_all)
    val selectRow: (String) -> String = labels.selectRow ?: format(string(R.string.monarch_datatable_select_row))
    /** The caller's count text, or `null` for the built-in plural `monarch_datatable_selected_count`. */
    val selectedCount: ((Int) -> String)? = labels.selectedCount
    val rowActions: (String) -> String = labels.rowActions ?: format(string(R.string.monarch_datatable_row_actions))
    val actionsText: String = labels.actionsText ?: string(R.string.monarch_datatable_actions_text)
    val open: String = labels.open ?: string(R.string.monarch_datatable_open)
    val sort: String = labels.sort ?: string(R.string.monarch_datatable_sort)
    val notSorted: String = labels.notSorted ?: string(R.string.monarch_datatable_not_sorted)
    val emptyCell: String = labels.emptyCell ?: string(R.string.monarch_datatable_empty_cell)
    val noValue: String = labels.noValue ?: string(R.string.monarch_datatable_no_value)
    val loading: String = labels.loading ?: string(R.string.monarch_datatable_loading)
    val emptyTitle: String = labels.emptyTitle ?: string(R.string.monarch_datatable_empty_title)
    val emptyMessage: String = labels.emptyMessage ?: string(R.string.monarch_datatable_empty_message)
    val filteredTitle: String = labels.filteredTitle ?: string(R.string.monarch_datatable_filtered_title)
    val filteredMessage: String = labels.filteredMessage ?: string(R.string.monarch_datatable_filtered_message)
    val errorTitle: String = labels.errorTitle ?: string(R.string.monarch_datatable_error_title)
    private val sortedAscending = labels.sortedAscending ?: string(R.string.monarch_datatable_sorted_ascending)
    private val sortedDescending = labels.sortedDescending ?: string(R.string.monarch_datatable_sorted_descending)
    private val markAscending = labels.ascendingMark ?: string(R.string.monarch_datatable_mark_ascending)
    private val markDescending = labels.descendingMark ?: string(R.string.monarch_datatable_mark_descending)

    /** The spoken sort state of a column: ascending, descending or not sorted. */
    fun sortState(direction: MonarchDataTableSortDirection?): String = when (direction) {
        MonarchDataTableSortDirection.Ascending -> sortedAscending
        MonarchDataTableSortDirection.Descending -> sortedDescending
        null -> notSorted
    }

    /** The visible mark after a sorted header, or an empty string. */
    fun mark(direction: MonarchDataTableSortDirection?): String = when (direction) {
        MonarchDataTableSortDirection.Ascending -> markAscending
        MonarchDataTableSortDirection.Descending -> markDescending
        null -> ""
    }

    private fun format(pattern: String): (String) -> String = { pattern.format(it) }
}

/** Every built-in string of a `MonarchDataTable`. */
private val DataTableStrings = listOf(
    R.string.monarch_datatable_select_all,
    R.string.monarch_datatable_select_row,
    R.string.monarch_datatable_row_actions,
    R.string.monarch_datatable_actions_text,
    R.string.monarch_datatable_open,
    R.string.monarch_datatable_sort,
    R.string.monarch_datatable_sorted_ascending,
    R.string.monarch_datatable_sorted_descending,
    R.string.monarch_datatable_not_sorted,
    R.string.monarch_datatable_mark_ascending,
    R.string.monarch_datatable_mark_descending,
    R.string.monarch_datatable_empty_cell,
    R.string.monarch_datatable_no_value,
    R.string.monarch_datatable_loading,
    R.string.monarch_datatable_empty_title,
    R.string.monarch_datatable_empty_message,
    R.string.monarch_datatable_filtered_title,
    R.string.monarch_datatable_filtered_message,
    R.string.monarch_datatable_error_title,
)

/** Reads the built-in strings of a `MonarchDataTable` and applies [labels]. */
@Composable
internal fun monarchDataTableTexts(labels: MonarchDataTableLabels): MonarchDataTableTexts {
    val strings = DataTableStrings.associateWith { stringResource(it) }
    return MonarchDataTableTexts(labels) { strings.getValue(it) }
}
