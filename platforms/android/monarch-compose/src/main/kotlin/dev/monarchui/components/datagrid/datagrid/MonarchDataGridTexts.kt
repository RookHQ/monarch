package dev.monarchui.components.datagrid.datagrid

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.monarchui.R
import dev.monarchui.components.datagrid.datatable.MonarchDataTableSortDirection
import dev.monarchui.components.datagrid.editablecell.MonarchEditableCellLabels

/**
 * The built-in strings of a `MonarchDataGrid`, with the caller's replacements applied.
 *
 * @param labels the caller's replacements.
 * @param string the built-in string of a resource id, read in composition.
 */
internal class MonarchDataGridTexts(labels: MonarchDataGridLabels, string: (Int) -> String) {
    val selectAll: String = labels.selectAll ?: string(R.string.monarch_datagrid_select_all)
    val selectRow: (String) -> String = labels.selectRow ?: format(string(R.string.monarch_datagrid_select_row))

    /** The caller's count text, or `null` for the built-in plural `monarch_datagrid_selected_count`. */
    val selectedCount: ((Int) -> String)? = labels.selectedCount
    val rowActions: (String) -> String = labels.rowActions ?: format(string(R.string.monarch_datagrid_row_actions))
    val actionsText: String = labels.actionsText ?: string(R.string.monarch_datagrid_actions_text)
    val columnOptions: String = labels.columnOptions ?: string(R.string.monarch_datagrid_column_options)
    val sortAscending: String = labels.sortAscending ?: string(R.string.monarch_datagrid_sort_ascending)
    val sortDescending: String = labels.sortDescending ?: string(R.string.monarch_datagrid_sort_descending)
    val clearSort: String = labels.clearSort ?: string(R.string.monarch_datagrid_clear_sort)
    val pinStart: String = labels.pinStart ?: string(R.string.monarch_datagrid_pin_start)
    val pinEnd: String = labels.pinEnd ?: string(R.string.monarch_datagrid_pin_end)
    val unpin: String = labels.unpin ?: string(R.string.monarch_datagrid_unpin)
    val hideColumn: String = labels.hideColumn ?: string(R.string.monarch_datagrid_hide_column)
    val widen: String = labels.widen ?: string(R.string.monarch_datagrid_widen)
    val narrow: String = labels.narrow ?: string(R.string.monarch_datagrid_narrow)
    val notSorted: String = labels.notSorted ?: string(R.string.monarch_datagrid_not_sorted)
    val editLabel: (String, String) -> String =
        labels.editLabel ?: format2(string(R.string.monarch_datagrid_edit_label))
    val cell: (String, String) -> String = labels.cell ?: format2(string(R.string.monarch_datagrid_cell))
    val open: String = labels.open ?: string(R.string.monarch_datagrid_open)
    val editRecord: String = labels.editRecord ?: string(R.string.monarch_datagrid_edit_record)
    val sort: String = labels.sort ?: string(R.string.monarch_datagrid_sort)
    val done: String = labels.done ?: string(R.string.monarch_datagrid_done)
    val emptyCell: String = string(R.string.monarch_datagrid_empty_cell)
    val noValue: String = labels.noValue ?: string(R.string.monarch_datagrid_no_value)
    val loading: String = labels.loading ?: string(R.string.monarch_datagrid_loading)
    val emptyTitle: String = labels.emptyTitle ?: string(R.string.monarch_datagrid_empty_title)
    val emptyMessage: String = labels.emptyMessage ?: string(R.string.monarch_datagrid_empty_message)
    val editableCell: MonarchEditableCellLabels = labels.editableCell
    private val sortedAscending = labels.sortedAscending ?: string(R.string.monarch_datagrid_sorted_ascending)
    private val sortedDescending = labels.sortedDescending ?: string(R.string.monarch_datagrid_sorted_descending)
    private val pinnedStart = labels.pinnedStart ?: string(R.string.monarch_datagrid_pinned_start)
    private val pinnedEnd = labels.pinnedEnd ?: string(R.string.monarch_datagrid_pinned_end)
    private val markAscending = string(R.string.monarch_datagrid_mark_ascending)
    private val markDescending = string(R.string.monarch_datagrid_mark_descending)

    /** The spoken sort state of a column: ascending, descending or not sorted. */
    fun sortState(direction: MonarchDataTableSortDirection?): String = when (direction) {
        MonarchDataTableSortDirection.Ascending -> sortedAscending
        MonarchDataTableSortDirection.Descending -> sortedDescending
        null -> notSorted
    }

    /** The spoken state of a header: its sort when sortable, then its pin. */
    fun headerState(sortable: Boolean, direction: MonarchDataTableSortDirection?, pin: MonarchDataGridPin?): String =
        listOfNotNull(
            sortState(direction).takeIf { sortable || direction != null },
            when (pin) {
                MonarchDataGridPin.Start -> pinnedStart
                MonarchDataGridPin.End -> pinnedEnd
                null -> null
            },
        ).joinToString(SEPARATOR)

    /** The visible mark after a sorted header, or an empty string. */
    fun mark(direction: MonarchDataTableSortDirection?): String = when (direction) {
        MonarchDataTableSortDirection.Ascending -> markAscending
        MonarchDataTableSortDirection.Descending -> markDescending
        null -> ""
    }

    /** How TalkBack reads a read-only cell of [column] holding [value]. */
    fun cellDescription(column: String, value: String): String = cell(column, value.ifBlank { noValue })

    private fun format(pattern: String): (String) -> String = { pattern.format(it) }

    private fun format2(pattern: String): (String, String) -> String = { a, b -> pattern.format(a, b) }

    private companion object {
        const val SEPARATOR = ", "
    }
}

/** Every built-in string of a `MonarchDataGrid`. */
private val DataGridStrings = listOf(
    R.string.monarch_datagrid_select_all,
    R.string.monarch_datagrid_select_row,
    R.string.monarch_datagrid_row_actions,
    R.string.monarch_datagrid_actions_text,
    R.string.monarch_datagrid_column_options,
    R.string.monarch_datagrid_sort_ascending,
    R.string.monarch_datagrid_sort_descending,
    R.string.monarch_datagrid_clear_sort,
    R.string.monarch_datagrid_pin_start,
    R.string.monarch_datagrid_pin_end,
    R.string.monarch_datagrid_unpin,
    R.string.monarch_datagrid_hide_column,
    R.string.monarch_datagrid_widen,
    R.string.monarch_datagrid_narrow,
    R.string.monarch_datagrid_sorted_ascending,
    R.string.monarch_datagrid_sorted_descending,
    R.string.monarch_datagrid_not_sorted,
    R.string.monarch_datagrid_pinned_start,
    R.string.monarch_datagrid_pinned_end,
    R.string.monarch_datagrid_mark_ascending,
    R.string.monarch_datagrid_mark_descending,
    R.string.monarch_datagrid_edit_label,
    R.string.monarch_datagrid_cell,
    R.string.monarch_datagrid_open,
    R.string.monarch_datagrid_edit_record,
    R.string.monarch_datagrid_sort,
    R.string.monarch_datagrid_done,
    R.string.monarch_datagrid_empty_cell,
    R.string.monarch_datagrid_no_value,
    R.string.monarch_datagrid_loading,
    R.string.monarch_datagrid_empty_title,
    R.string.monarch_datagrid_empty_message,
)

/** Reads the built-in strings of a `MonarchDataGrid` and applies [labels]. */
@Composable
internal fun monarchDataGridTexts(labels: MonarchDataGridLabels): MonarchDataGridTexts {
    val strings = DataGridStrings.associateWith { stringResource(it) }
    return MonarchDataGridTexts(labels) { strings.getValue(it) }
}
