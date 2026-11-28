package dev.monarchui.components.datagrid.datagrid

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import dev.monarchui.components.datagrid.datatable.MonarchDataTableRowAction
import dev.monarchui.components.datagrid.datatable.MonarchDataTableRules
import dev.monarchui.components.datagrid.datatable.MonarchDataTableSort
import dev.monarchui.components.datagrid.datatable.MonarchDataTableSortDirection

/** The selection, its callback and the keys of the rows that can be selected. */
internal class MonarchDataGridSelector<K>(
    val selection: Set<K>,
    val onSelectionChange: (Set<K>) -> Unit,
    val selectable: List<K>,
) {
    /** The selection after the select-all control: every selectable key, or none when all were selected. */
    fun toggledAll(): Set<K> = MonarchDataTableRules.toggledAll(selection, selectable)

    /** The state of the select-all control. */
    val allState get() = MonarchDataTableRules.selectAllState(selection, selectable)
}

/** The sort and its callback. */
internal class MonarchDataGridSorting(
    val sort: MonarchDataTableSort?,
    val onSortChange: ((MonarchDataTableSort?) -> Unit)?,
)

/** The column state the app hoists: hidden columns, pins and widths, with their callbacks. */
internal class MonarchDataGridColumnState(
    val hidden: Set<String>,
    val onHiddenChange: ((Set<String>) -> Unit)?,
    val pinning: Map<String, MonarchDataGridPin>?,
    val onPinningChange: ((Map<String, MonarchDataGridPin>) -> Unit)?,
    val widths: Map<String, Dp>,
    val onWidthsChange: ((Map<String, Dp>) -> Unit)?,
)

/** What a row offers besides its cells. */
internal class MonarchDataGridRowBehavior<T>(
    val rowLabel: ((T) -> String)?,
    val rowSelectable: (T) -> Boolean,
    val rowActions: ((T) -> List<MonarchDataTableRowAction>)?,
    val onRowOpen: ((T) -> Unit)?,
    val actionsIcon: (@Composable () -> Unit)?,
)

/** Everything the rows and headers of a grid need, resolved once per composition. */
@Suppress("LongParameterList")
internal class MonarchDataGridSetup<T, K : Any>(
    val rows: List<T>,
    val columns: List<MonarchDataGridColumn<T>>,
    val arrangement: MonarchDataGridArrangement<T>,
    val rowKey: (T) -> K,
    val behavior: MonarchDataGridRowBehavior<T>,
    val selector: MonarchDataGridSelector<K>?,
    val sorting: MonarchDataGridSorting,
    val columnState: MonarchDataGridColumnState,
    val density: MonarchDataGridDensity,
    val texts: MonarchDataGridTexts,
) {
    val hasActions: Boolean get() = behavior.rowActions != null

    /** A row's name: the row label, or the text of the first shown column. */
    fun name(row: T): String =
        behavior.rowLabel?.invoke(row) ?: arrangement.all.firstOrNull()?.value?.invoke(row).orEmpty()

    /** Whether [row] is selected, or `null` when it has no checkbox. */
    fun selected(row: T): Boolean? {
        val selector = selector ?: return null
        return (rowKey(row) in selector.selection).takeIf { behavior.rowSelectable(row) }
    }

    /** Selects or deselects [row]. */
    fun select(row: T, on: Boolean) {
        val selector = selector ?: return
        val key = rowKey(row)
        selector.onSelectionChange(if (on) selector.selection + key else selector.selection - key)
    }

    /** Whether [row] has a cell that can be edited among the shown columns. */
    fun editable(row: T): Boolean = arrangement.all.any { column -> column.edit?.editable?.invoke(row) == true }

    /** The width of [column]. */
    fun widthOf(column: MonarchDataGridColumn<T>): Dp = MonarchDataGridRules.widthOf(column, columnState.widths)

    /** The sort direction of [column], or `null` when another column or nothing is sorted. */
    fun directionOf(column: MonarchDataGridColumn<T>): MonarchDataTableSortDirection? =
        sorting.sort?.takeIf { it.columnId == column.id }?.direction

    /** The pin of [column] as the app or the column sets it. */
    fun pinOf(column: MonarchDataGridColumn<T>): MonarchDataGridPin? =
        MonarchDataGridRules.pinOf(column, columnState.pinning)

    /** The items of [column]'s menu. */
    fun menuItems(column: MonarchDataGridColumn<T>): List<MonarchDataGridMenuItem> = monarchDataGridMenuItems(
        column = column,
        sorted = directionOf(column),
        pin = pinOf(column),
        width = widthOf(column),
        abilities = MonarchDataGridMenuAbilities(
            sort = sorting.onSortChange != null,
            pin = columnState.onPinningChange != null,
            hide = columnState.onHiddenChange != null,
            resize = columnState.onWidthsChange != null,
        ),
    )

    /** Runs a column menu [item] for [column]. */
    fun choose(column: MonarchDataGridColumn<T>, item: MonarchDataGridMenuItem) {
        val state = columnState
        when (item) {
            MonarchDataGridMenuItem.SortAscending -> sorting.onSortChange?.invoke(MonarchDataTableSort(column.id))
            MonarchDataGridMenuItem.SortDescending -> sorting.onSortChange?.invoke(
                MonarchDataTableSort(column.id, MonarchDataTableSortDirection.Descending),
            )
            MonarchDataGridMenuItem.ClearSort -> sorting.onSortChange?.invoke(null)
            MonarchDataGridMenuItem.PinStart -> pin(column, MonarchDataGridPin.Start)
            MonarchDataGridMenuItem.PinEnd -> pin(column, MonarchDataGridPin.End)
            MonarchDataGridMenuItem.Unpin -> pin(column, null)
            MonarchDataGridMenuItem.Hide -> state.onHiddenChange?.invoke(state.hidden + column.id)
            MonarchDataGridMenuItem.Widen -> state.onWidthsChange?.invoke(
                MonarchDataGridRules.resized(column, state.widths, MonarchDataGridTokens.ResizeStep),
            )
            MonarchDataGridMenuItem.Narrow -> state.onWidthsChange?.invoke(
                MonarchDataGridRules.resized(column, state.widths, -MonarchDataGridTokens.ResizeStep),
            )
        }
    }

    private fun pin(column: MonarchDataGridColumn<T>, pin: MonarchDataGridPin?) {
        columnState.onPinningChange?.invoke(MonarchDataGridRules.pinned(columns, columnState.pinning, column.id, pin))
    }

    /** The label of a column menu [item]. */
    fun labelOf(item: MonarchDataGridMenuItem): String = when (item) {
        MonarchDataGridMenuItem.SortAscending -> texts.sortAscending
        MonarchDataGridMenuItem.SortDescending -> texts.sortDescending
        MonarchDataGridMenuItem.ClearSort -> texts.clearSort
        MonarchDataGridMenuItem.PinStart -> texts.pinStart
        MonarchDataGridMenuItem.PinEnd -> texts.pinEnd
        MonarchDataGridMenuItem.Unpin -> texts.unpin
        MonarchDataGridMenuItem.Hide -> texts.hideColumn
        MonarchDataGridMenuItem.Widen -> texts.widen
        MonarchDataGridMenuItem.Narrow -> texts.narrow
    }
}
