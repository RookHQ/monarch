package com.rookhq.monarch.components.datagrid.datagrid

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableColumn
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableLayout
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableRules
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableSort
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableSortDirection

/** The shown columns of a grid, split by where they sit. */
internal class MonarchDataGridArrangement<T>(
    val start: List<MonarchDataGridColumn<T>>,
    val middle: List<MonarchDataGridColumn<T>>,
    val end: List<MonarchDataGridColumn<T>>,
) {
    /** Every shown column in display order. */
    val all: List<MonarchDataGridColumn<T>> get() = start + middle + end
}

/** The pure rules of MonarchDataGrid: layout, columns, pins, widths, sorting. */
internal object MonarchDataGridRules {
    /** Whether [layout] at [fontScale] draws the grid rather than a list of records. */
    fun isGrid(layout: MonarchDataTableLayout, fontScale: Float): Boolean =
        MonarchDataTableRules.isTable(layout, fontScale)

    /** The pin of [column]: from [pinning] when the app passes it, otherwise from the column. */
    fun <T> pinOf(column: MonarchDataGridColumn<T>, pinning: Map<String, MonarchDataGridPin>?): MonarchDataGridPin? =
        if (pinning != null) pinning[column.id] else column.pinned

    /** Every column's pin as a map, the form `onPinningChange` receives. */
    fun <T> pinningOf(
        columns: List<MonarchDataGridColumn<T>>,
        pinning: Map<String, MonarchDataGridPin>?,
    ): Map<String, MonarchDataGridPin> =
        pinning ?: columns.mapNotNull { column -> column.pinned?.let { column.id to it } }.toMap()

    /** The pinning after [columnId] is pinned to [pin], or unpinned with `null`. */
    fun <T> pinned(
        columns: List<MonarchDataGridColumn<T>>,
        pinning: Map<String, MonarchDataGridPin>?,
        columnId: String,
        pin: MonarchDataGridPin?,
    ): Map<String, MonarchDataGridPin> {
        val current = pinningOf(columns, pinning)
        return if (pin == null) current - columnId else current + (columnId to pin)
    }

    /** The width of [column]: the app's width, the column's own, or the default. */
    fun <T> widthOf(column: MonarchDataGridColumn<T>, widths: Map<String, Dp>): Dp =
        widths[column.id] ?: column.width.takeIf { it.isSpecified } ?: MonarchDataGridTokens.ColumnWidth

    /** The widths after [column] grows by [delta] (negative to shrink), kept between the minimum and maximum. */
    fun <T> resized(column: MonarchDataGridColumn<T>, widths: Map<String, Dp>, delta: Dp): Map<String, Dp> {
        val next = (widthOf(column, widths) + delta)
            .coerceIn(MonarchDataGridTokens.MinColumnWidth, MonarchDataGridTokens.MaxColumnWidth)
        return widths + (column.id to next)
    }

    /**
     * The shown columns in display order, split into start-pinned, scrolling and end-pinned ones. When the
     * pinned columns together would take more than half of [viewport], every column scrolls, so a narrow screen
     * is never filled by pinned columns.
     */
    @Suppress("LongParameterList")
    fun <T> arranged(
        columns: List<MonarchDataGridColumn<T>>,
        hidden: Set<String>,
        order: List<String>,
        pinning: Map<String, MonarchDataGridPin>?,
        widths: Map<String, Dp>,
        viewport: Dp,
    ): MonarchDataGridArrangement<T> {
        val byId = columns.associateBy { it.id }
        val shown = MonarchDataTableRules.shownColumns(asTableColumns(columns), hidden, order)
            .mapNotNull { byId[it.id] }
        val start = shown.filter { pinOf(it, pinning) == MonarchDataGridPin.Start }
        val end = shown.filter { pinOf(it, pinning) == MonarchDataGridPin.End }
        val pinnedWidth = (start + end).fold(Dp(0f)) { sum, column -> sum + widthOf(column, widths) }
        if (pinnedWidth > viewport * MonarchDataGridTokens.MAX_PINNED_SHARE) {
            return MonarchDataGridArrangement(emptyList(), shown, emptyList())
        }
        return MonarchDataGridArrangement(start, shown.filter { pinOf(it, pinning) == null }, end)
    }

    /** [rows] in [sort] order, stable; unchanged without a sort or for an unknown or unsortable column. */
    fun <T> sortedRows(rows: List<T>, columns: List<MonarchDataGridColumn<T>>, sort: MonarchDataTableSort?): List<T> =
        MonarchDataTableRules.sortedRows(rows, asTableColumns(columns), sort)

    /** The columns as `MonarchDataTableColumn`s, to share DataTable's ordering and sorting rules. */
    fun <T> asTableColumns(columns: List<MonarchDataGridColumn<T>>): List<MonarchDataTableColumn<T>> =
        columns.map { column ->
            MonarchDataTableColumn(
                title = column.title,
                id = column.id,
                numeric = column.numeric,
                sortable = column.sortable,
                hideable = column.hideable,
                comparator = column.comparator,
                value = column.value,
            )
        }
}

/** An item of a grid's column menu. */
internal enum class MonarchDataGridMenuItem {
    SortAscending,
    SortDescending,
    ClearSort,
    PinStart,
    PinEnd,
    Unpin,
    Hide,
    Widen,
    Narrow,
}

/** What the app lets the column menu change: each is `true` when the app passed the callback. */
internal class MonarchDataGridMenuAbilities(
    val sort: Boolean,
    val pin: Boolean,
    val hide: Boolean,
    val resize: Boolean,
)

/** The items of the column menu of [column], given its sort, pin and width. */
internal fun <T> monarchDataGridMenuItems(
    column: MonarchDataGridColumn<T>,
    sorted: MonarchDataTableSortDirection?,
    pin: MonarchDataGridPin?,
    width: Dp,
    abilities: MonarchDataGridMenuAbilities,
): List<MonarchDataGridMenuItem> = buildList {
    if (abilities.sort && column.sortable) addAll(sortItems(sorted))
    if (abilities.pin && column.pinnable) addAll(pinItems(pin))
    if (abilities.hide && column.hideable) add(MonarchDataGridMenuItem.Hide)
    if (abilities.resize) addAll(resizeItems(width))
}

private fun sortItems(sorted: MonarchDataTableSortDirection?) = buildList {
    if (sorted != MonarchDataTableSortDirection.Ascending) add(MonarchDataGridMenuItem.SortAscending)
    if (sorted != MonarchDataTableSortDirection.Descending) add(MonarchDataGridMenuItem.SortDescending)
    if (sorted != null) add(MonarchDataGridMenuItem.ClearSort)
}

private fun pinItems(pin: MonarchDataGridPin?) = buildList {
    if (pin != MonarchDataGridPin.Start) add(MonarchDataGridMenuItem.PinStart)
    if (pin != MonarchDataGridPin.End) add(MonarchDataGridMenuItem.PinEnd)
    if (pin != null) add(MonarchDataGridMenuItem.Unpin)
}

private fun resizeItems(width: Dp) = buildList {
    if (width < MonarchDataGridTokens.MaxColumnWidth) add(MonarchDataGridMenuItem.Widen)
    if (width > MonarchDataGridTokens.MinColumnWidth) add(MonarchDataGridMenuItem.Narrow)
}
