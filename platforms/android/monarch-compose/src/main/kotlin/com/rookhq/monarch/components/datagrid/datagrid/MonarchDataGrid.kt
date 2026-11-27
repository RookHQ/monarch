package com.rookhq.monarch.components.datagrid.datagrid

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableLayout
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableRowAction
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableSort
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingState
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingStateVariant
import com.rookhq.monarch.internal.MonarchDefaultIcons
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * An editable grid of records for tablets and hardware keyboards, and a list of records with an editor on
 * phones: columns that sort, pin, hide and resize from a menu on their header, cells that edit in place,
 * selection, row actions and loading more rows at the end.
 *
 * Built by Monarch on a `LazyColumn`: Compose and Material 3 have no data grid. In the grid layout each column
 * has a fixed width; pinned columns stay at the start or end edge while the other columns scroll sideways
 * together, through one shared `ScrollState`. Tapping a header opens a Material 3 `DropdownMenu` with the
 * actions the app allows (sort, pin, hide, widen and narrow). A column with an [MonarchDataGridColumn.edit]
 * shows a `MonarchEditableCell` in each row; other cells are focusable text read as "Column: value" and open
 * the row with [onRowOpen]. In the list layout (phones and large text) each record is one TalkBack element;
 * tapping a record with editable cells opens a Material 3 `ModalBottomSheet` with an editable cell per column.
 *
 * Every piece of state is hoisted: the sort, the selection, the hidden columns, the order, the pins and the
 * widths. A change the app does not take a callback for is not offered. The rows are lazy and the grid scrolls
 * itself: bound its height inside a scrolling parent. Row keys are also the lazy keys, so they must be types
 * Android can save in a `Bundle` (a `String`, `Int` or `Long`).
 *
 * With a hardware keyboard, Tab and the arrow keys move focus between cells through the Compose focus system,
 * Enter opens or edits the focused cell, Space toggles the focused row's selection and Ctrl+A (or Meta+A)
 * selects or clears all rows.
 *
 * @param label names the grid for TalkBack, such as "Invoices".
 * @param rows the rows, in the app's order.
 * @param columns every column in its default order.
 * @param rowKey a row's stable, unique key; also the key in [selection].
 * @param modifier the modifier of the grid.
 * @param layout a grid, a list of records, or [MonarchDataTableLayout.Automatic] by text size.
 * @param density the padding around cells; both keep 48dp touch targets.
 * @param sort the sort, one column and a direction, or `null` for the app's order.
 * @param onSortChange called with the new sort, or `null` when the sort is cleared. Without it nothing sorts.
 * @param sortsRows whether the grid sorts [rows] itself; `false` when the server sorts.
 * @param selection the selected keys. With it and [onSelectionChange], rows get checkboxes.
 * @param onSelectionChange called with the whole new selection.
 * @param hiddenColumns ids of hidden columns.
 * @param onHiddenColumnsChange called with the new hidden set by Hide column in a column menu.
 * @param columnOrder column ids in display order; missing ids follow in their default order.
 * @param pinning every pinned column by id, or `null` to use each column's own pin.
 * @param onPinningChange called with every pinned column by Pin to start, Pin to end and Unpin.
 * @param columnWidths widths by column id, replacing the columns' own widths.
 * @param onColumnWidthsChange called with the new widths by Widen column and Narrow column.
 * @param loading without rows shows placeholder rows; with rows shows a progress bar above them.
 * @param rowLabel a row's name in spoken labels and in the list layout. By default the first shown cell.
 * @param rowSelectable whether a row has a checkbox.
 * @param rowActions a row's actions: an actions menu and TalkBack actions.
 * @param onRowOpen opens a row: a tap on a read-only cell, or Open details in the record editor.
 * @param onEndReached called when the last row comes into view, to load more rows.
 * @param actionsIcon the actions button's icon, for example a three-dot mark. Without it the button shows
 * "Actions". Monarch bundles no icons.
 * @param emptyState replaces the built-in empty state shown without rows.
 * @param state the scroll state of the rows.
 * @param labels replacements for the built-in strings.
 */
@Composable
@Suppress("LongParameterList", "LongMethod")
public fun <T, K : Any> MonarchDataGrid(
    label: String,
    rows: List<T>,
    columns: List<MonarchDataGridColumn<T>>,
    rowKey: (T) -> K,
    modifier: Modifier = Modifier,
    layout: MonarchDataTableLayout = MonarchDataTableLayout.Automatic,
    density: MonarchDataGridDensity = MonarchDataGridDensity.Comfortable,
    sort: MonarchDataTableSort? = null,
    onSortChange: ((MonarchDataTableSort?) -> Unit)? = null,
    sortsRows: Boolean = true,
    selection: Set<K>? = null,
    onSelectionChange: ((Set<K>) -> Unit)? = null,
    hiddenColumns: Set<String> = emptySet(),
    onHiddenColumnsChange: ((Set<String>) -> Unit)? = null,
    columnOrder: List<String> = emptyList(),
    pinning: Map<String, MonarchDataGridPin>? = null,
    onPinningChange: ((Map<String, MonarchDataGridPin>) -> Unit)? = null,
    columnWidths: Map<String, Dp> = emptyMap(),
    onColumnWidthsChange: ((Map<String, Dp>) -> Unit)? = null,
    loading: Boolean = false,
    rowLabel: ((T) -> String)? = null,
    rowSelectable: (T) -> Boolean = { true },
    rowActions: ((T) -> List<MonarchDataTableRowAction>)? = null,
    onRowOpen: ((T) -> Unit)? = null,
    onEndReached: (() -> Unit)? = null,
    actionsIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.More,
    emptyState: (@Composable () -> Unit)? = null,
    state: LazyListState = rememberLazyListState(),
    labels: MonarchDataGridLabels = MonarchDataGridLabels(),
) {
    val texts = monarchDataGridTexts(labels)
    val selector = monarchDataGridSelector(rows, rowKey, selection, onSelectionChange, rowSelectable)
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(
        modifier = modifier
            .semantics {
                isTraversalGroup = true
                contentDescription = label
            }
            .selectAllKey(selector),
    ) {
        val setup = MonarchDataGridSetup(
            rows = if (sortsRows) MonarchDataGridRules.sortedRows(rows, columns, sort) else rows,
            columns = columns,
            arrangement = MonarchDataGridRules.arranged(
                columns = columns,
                hidden = hiddenColumns,
                order = columnOrder,
                pinning = pinning,
                widths = columnWidths,
                viewport = maxWidth,
            ),
            rowKey = rowKey,
            behavior = MonarchDataGridRowBehavior(rowLabel, rowSelectable, rowActions, onRowOpen, actionsIcon),
            selector = selector,
            sorting = MonarchDataGridSorting(sort, onSortChange),
            columnState = MonarchDataGridColumnState(
                hidden = hiddenColumns,
                onHiddenChange = onHiddenColumnsChange,
                pinning = pinning,
                onPinningChange = onPinningChange,
                widths = columnWidths,
                onWidthsChange = onColumnWidthsChange,
            ),
            density = density,
            texts = texts,
        )
        when {
            rows.isEmpty() && loading -> MonarchLoadingState(
                label = texts.loading,
                variant = MonarchLoadingStateVariant.Rows,
                rows = MonarchDataGridTokens.SKELETON_ROWS,
            )
            rows.isEmpty() -> emptyState?.invoke() ?: MonarchEmptyState(
                title = texts.emptyTitle,
                message = texts.emptyMessage,
            )
            MonarchDataGridRules.isGrid(layout, fontScale) -> MonarchDataGridTable(setup, state, loading)
            else -> MonarchDataGridRecords(setup, state, loading)
        }
    }
    if (onEndReached != null) MonarchDataGridEndReached(state, rows.size, onEndReached)
}

/** The selection helper, or `null` when the grid does not select. */
private fun <T, K : Any> monarchDataGridSelector(
    rows: List<T>,
    rowKey: (T) -> K,
    selection: Set<K>?,
    onSelectionChange: ((Set<K>) -> Unit)?,
    rowSelectable: (T) -> Boolean,
): MonarchDataGridSelector<K>? = if (selection != null && onSelectionChange != null) {
    MonarchDataGridSelector(selection, onSelectionChange, rows.filter(rowSelectable).map(rowKey))
} else {
    null
}

/** Ctrl+A or Meta+A selects or clears all rows. */
private fun <K> Modifier.selectAllKey(selector: MonarchDataGridSelector<K>?): Modifier = onKeyEvent { event ->
    val all = selector != null && event.type == KeyEventType.KeyDown && event.key == Key.A &&
        (event.isCtrlPressed || event.isMetaPressed)
    if (all) selector.onSelectionChange(selector.toggledAll())
    all
}

/** Calls [onEndReached] each time the last item comes into view, and again after more rows arrive. */
@Composable
private fun MonarchDataGridEndReached(state: LazyListState, rowCount: Int, onEndReached: () -> Unit) {
    val current by rememberUpdatedState(onEndReached)
    LaunchedEffect(state, rowCount) {
        snapshotFlow {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 1
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { current() }
    }
}
