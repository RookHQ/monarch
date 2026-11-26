package com.rookhq.monarch.components.datagrid.datatable

import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.state.ToggleableState
import com.rookhq.monarch.components.datagrid.columnchooser.MonarchColumnChooserColumn
import com.rookhq.monarch.components.datagrid.columnchooser.MonarchColumnChooserTokens
import java.text.Collator

/** The pure rules of MonarchDataTable: layout, shown columns, sorting and selection. */
internal object MonarchDataTableRules {
    /** Saves the table's own sort as its column id and direction name. */
    val SortSaver: Saver<MonarchDataTableSort?, Any> = Saver(
        save = { sort -> sort?.let { listOf(it.columnId, it.direction.name) } ?: emptyList<String>() },
        restore = { saved ->
            (saved as List<*>).takeIf { it.size == 2 }?.let {
                MonarchDataTableSort(it[0] as String, MonarchDataTableSortDirection.valueOf(it[1] as String))
            }
        },
    )

    /** Whether [layout] at [fontScale] draws a table rather than a list of records. */
    fun isTable(layout: MonarchDataTableLayout, fontScale: Float): Boolean = when (layout) {
        MonarchDataTableLayout.Table -> true
        MonarchDataTableLayout.List -> false
        MonarchDataTableLayout.Automatic -> fontScale < MonarchDataTableTokens.LARGE_TEXT_SCALE
    }

    /** The shown columns in display order: [order] first, the rest in their default order, without hidden ones. */
    fun <T> shownColumns(
        columns: List<MonarchDataTableColumn<T>>,
        hidden: Set<String>,
        order: List<String>,
    ): List<MonarchDataTableColumn<T>> {
        val byId = columns.associateBy { it.id }
        val chooser = columns.map { MonarchColumnChooserColumn(it.title, it.id, it.hideable) }
        return MonarchColumnChooserTokens.ordered(chooser, order)
            .filter { MonarchColumnChooserTokens.isShown(it, hidden) }
            .mapNotNull { byId[it.id] }
    }

    /** [rows] in [sort] order, stable; unchanged without a sort or for an unknown or unsortable column. */
    fun <T> sortedRows(
        rows: List<T>,
        columns: List<MonarchDataTableColumn<T>>,
        sort: MonarchDataTableSort?,
    ): List<T> {
        val column = columns.firstOrNull { it.id == sort?.columnId && it.sortable } ?: return rows
        val ascending = column.comparator ?: defaultComparator(column)
        val empty = compareBy<T> { column.value(it).isBlank() }
        val descending = sort?.direction == MonarchDataTableSortDirection.Descending
        val ordered = if (descending) ascending.reversed() else ascending
        return rows.sortedWith(empty.then(ordered))
    }

    /** The default ascending order of a column: by number when numeric, otherwise by text ignoring case. */
    fun <T> defaultComparator(column: MonarchDataTableColumn<T>): Comparator<T> {
        if (column.numeric) {
            return compareBy<T, Double?>(nullsLast()) { numberOf(column.value(it)) }
        }
        val collator = Collator.getInstance().apply { strength = Collator.SECONDARY }
        return Comparator { a, b -> collator.compare(column.value(a), column.value(b)) }
    }

    /** The sort after tapping the header of [columnId]: ascending first, then the other direction. */
    fun nextSort(current: MonarchDataTableSort?, columnId: String): MonarchDataTableSort =
        if (current?.columnId == columnId) current.toggled() else MonarchDataTableSort(columnId)

    /** The state of the select-all checkbox from the selected and the selectable keys. */
    fun <K> selectAllState(selection: Set<K>, selectable: List<K>): ToggleableState {
        val selected = selectable.count { it in selection }
        return when {
            selected == 0 -> ToggleableState.Off
            selected == selectable.size -> ToggleableState.On
            else -> ToggleableState.Indeterminate
        }
    }

    /** The selection after the select-all checkbox: every selectable key, or none of them when all were selected. */
    fun <K> toggledAll(selection: Set<K>, selectable: List<K>): Set<K> =
        if (selectable.isNotEmpty() && selectable.all { it in selection }) {
            selection - selectable.toSet()
        } else {
            selection + selectable
        }

    /** The number in a cell's text, ignoring grouping marks, currency and units: "€1,200.50" is 1200.5. */
    fun numberOf(text: String): Double? = text.filter { it.isDigit() || it in NUMBER_MARKS }.toDoubleOrNull()

    private const val NUMBER_MARKS = ".-"
}
