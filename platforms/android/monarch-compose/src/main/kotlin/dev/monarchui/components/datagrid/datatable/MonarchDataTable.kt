package dev.monarchui.components.datagrid.datatable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateKind
import dev.monarchui.components.feedback.errorstate.MonarchErrorState
import dev.monarchui.internal.MonarchDefaultIcons

/**
 * Records in columns: a table that scrolls sideways when the columns need more
 * room, and a list of records with a labelled line per field at large text sizes, with sorting, selection, row actions,
 * column visibility and order, and the loading, empty, filtered and error
 * states. Read-only: cells are text.
 *
 * Built by Monarch on a `LazyColumn`: Compose has no data table. In the
 * table layout a header row names the columns, and sortable headers are
 * buttons with the sort as their state. In the list layout each record is
 * one TalkBack element read as its name and "Field, value" per column, and a
 * Sort menu sits above the records. With [selection] each row has a Material
 * 3 `Checkbox` and a select-all checkbox with the count sits above the rows.
 * [rowActions] give each row an actions menu and the same actions as TalkBack
 * actions. Without rows, [loading], [errorMessage] and [filtered] pick a
 * skeleton, a `MonarchErrorState` or a filtered `MonarchEmptyState`.
 *
 * Sorting, selection, hidden columns and the order are hoisted. The table is
 * lazy and scrolls itself: bound its height inside a scrolling parent. Row
 * keys are also the lazy keys, so they must be types Android can save in a
 * `Bundle` (a `String`, `Int` or `Long`).
 *
 * @param caption names the table for TalkBack, such as "Open tickets".
 * @param rows the rows, in the app's order.
 * @param columns every column in its default order.
 * @param rowKey a row's stable, unique key; also the key in [selection].
 * @param modifier the modifier of the table.
 * @param sort the sort, one column and a direction, or `null` for the app's order. Without [onSortChange] it is
 * the first sort of the table's own sort state.
 * @param onSortChange called with the new sort when a header or the Sort menu is used. Without it, and with
 * [sortsRows], the table keeps the sort itself (saved across configuration changes), as on iOS; with neither,
 * nothing sorts.
 * @param selection the selected keys. With it and [onSelectionChange], rows get checkboxes.
 * @param onSelectionChange called with the whole new selection.
 * @param hiddenColumns ids of hidden columns, as `MonarchColumnChooser` writes them.
 * @param columnOrder column ids in display order; missing ids follow in their default order.
 * @param layout a table, a list of records, or [MonarchDataTableLayout.Automatic] by text size.
 * @param sortsRows whether the table sorts [rows] itself; `false` when the server sorts.
 * @param loading without rows shows placeholder rows; with rows adds them after the last row.
 * @param errorMessage without rows shows the error state with this message.
 * @param filtered without rows the filters matched nothing: shows the filtered empty state.
 * @param rowLabel a row's name in the list layout and in spoken labels. By default the first shown cell.
 * @param rowSelectable whether a row has a checkbox.
 * @param rowActions a row's actions: an actions menu and TalkBack actions.
 * @param onRetry adds a retry button to the error state.
 * @param onClearFilters adds Clear filters to the filtered empty state.
 * @param onOpen makes each row a button that opens the record.
 * @param actionsIcon the actions button's icon, for example a three-dot mark. Without it the button shows
 * "Actions". Monarch bundles no icons.
 * @param state the scroll state of the rows.
 * @param labels replacements for the built-in strings.
 * @param footer content after the last row, for example a `MonarchLoadMore` for paged data.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun <T, K : Any> MonarchDataTable(
    caption: String,
    rows: List<T>,
    columns: List<MonarchDataTableColumn<T>>,
    rowKey: (T) -> K,
    modifier: Modifier = Modifier,
    sort: MonarchDataTableSort? = null,
    onSortChange: ((MonarchDataTableSort) -> Unit)? = null,
    selection: Set<K>? = null,
    onSelectionChange: ((Set<K>) -> Unit)? = null,
    hiddenColumns: Set<String> = emptySet(),
    columnOrder: List<String> = emptyList(),
    layout: MonarchDataTableLayout = MonarchDataTableLayout.Automatic,
    sortsRows: Boolean = true,
    loading: Boolean = false,
    errorMessage: String? = null,
    filtered: Boolean = false,
    rowLabel: ((T) -> String)? = null,
    rowSelectable: (T) -> Boolean = { true },
    rowActions: ((T) -> List<MonarchDataTableRowAction>)? = null,
    onRetry: (() -> Unit)? = null,
    onClearFilters: (() -> Unit)? = null,
    onOpen: ((T) -> Unit)? = null,
    actionsIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.More,
    state: LazyListState = rememberLazyListState(),
    labels: MonarchDataTableLabels = MonarchDataTableLabels(),
    footer: (@Composable () -> Unit)? = null,
) {
    val texts = monarchDataTableTexts(labels)
    // The table's own sort, when the app does not hoist it (iOS: a nil binding).
    var ownSort by rememberSaveable(stateSaver = MonarchDataTableRules.SortSaver) { mutableStateOf(sort) }
    val ownsSort = onSortChange == null && sortsRows
    val shownSort = if (ownsSort) ownSort else sort
    val sortChange: ((MonarchDataTableSort) -> Unit)? = onSortChange ?: if (sortsRows) {
        { next -> ownSort = next }
    } else {
        null
    }
    val shownColumns = MonarchDataTableRules.shownColumns(columns, hiddenColumns, columnOrder)
    val selector = if (selection != null && onSelectionChange != null) {
        DataTableSelector(selection, onSelectionChange, rows.filter(rowSelectable).map(rowKey))
    } else {
        null
    }
    val setup = DataTableSetup(
        rows = if (sortsRows) MonarchDataTableRules.sortedRows(rows, columns, shownSort) else rows,
        columns = shownColumns,
        rowKey = rowKey,
        rowLabel = rowLabel,
        rowSelectable = rowSelectable,
        behavior = DataTableRowBehavior(rowActions, onOpen, actionsIcon),
        selector = selector,
        sorting = DataTableSorting(shownSort, sortChange),
        texts = texts,
    )
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(
        modifier = modifier.semantics {
            isTraversalGroup = true
            contentDescription = caption
        },
    ) {
        val table = MonarchDataTableRules.isTable(layout, fontScale)
        if (rows.isEmpty()) {
            NoRows(loading, errorMessage, filtered, onRetry, onClearFilters, texts)
        } else {
            val widths = rememberDataTableWidths(
                columns = shownColumns,
                rows = rows,
                available = maxWidth,
                selecting = selector != null,
                hasActions = rowActions != null,
                floor = LocalMonarchDataTableMinColumnWidth.current ?: Dp(0f),
            )
            val scroll = rememberScrollState()
            val width = if (table) widths.needed else maxWidth
            LazyColumn(
                modifier = (if (table) Modifier.horizontalScroll(scroll) else Modifier).width(width),
                state = state,
            ) {
                dataTableItems(setup, table, widths.weights)
                if (loading) item(key = "monarch-datatable-loading") { MonarchDataTableLoadingRows(texts.loading) }
                if (footer != null) item(key = "monarch-datatable-footer") { footer() }
            }
        }
    }
}

/** The selection, its callback and the keys of the rows that can be selected. */
private class DataTableSelector<K>(
    val selection: Set<K>,
    val onSelectionChange: (Set<K>) -> Unit,
    val selectable: List<K>,
)

/** Everything the rows of a table need, resolved once per composition. */
private class DataTableSetup<T, K : Any>(
    val rows: List<T>,
    val columns: List<MonarchDataTableColumn<T>>,
    val rowKey: (T) -> K,
    val rowLabel: ((T) -> String)?,
    val rowSelectable: (T) -> Boolean,
    val behavior: DataTableRowBehavior<T>,
    val selector: DataTableSelector<K>?,
    val sorting: DataTableSorting,
    val texts: MonarchDataTableTexts,
) {
    val rowActions: ((T) -> List<MonarchDataTableRowAction>)? get() = behavior.rowActions
    val actionsIcon: (@Composable () -> Unit)? get() = behavior.actionsIcon
    val sort: MonarchDataTableSort? get() = sorting.sort
    val onSortChange: ((MonarchDataTableSort) -> Unit)? get() = sorting.onSortChange

    /** Whether the row's name comes from rowLabel rather than the first shown column. */
    val namedApart: Boolean get() = rowLabel != null

    private fun rowName(row: T): String = rowLabel?.invoke(row) ?: columns.firstOrNull()?.value(row).orEmpty()

    /** How [row] is drawn and what it can do. */
    fun model(row: T): MonarchDataTableRowModel {
        val key = rowKey(row)
        val selector = selector
        return MonarchDataTableRowModel(
            name = rowName(row),
            selected = selector?.let { key in it.selection }?.takeIf { rowSelectable(row) },
            onSelectedChange = selector?.let { s ->
                { on: Boolean -> s.onSelectionChange(if (on) s.selection + key else s.selection - key) }
            },
            actions = rowActions?.invoke(row).orEmpty(),
            onOpen = behavior.onOpen?.let { open -> { open(row) } },
        )
    }
}

/** What a row offers besides its cells: actions, opening, and the actions button's icon. */
private class DataTableRowBehavior<T>(
    val rowActions: ((T) -> List<MonarchDataTableRowAction>)?,
    val onOpen: ((T) -> Unit)?,
    val actionsIcon: (@Composable () -> Unit)?,
)

/** The sort and its callback. */
private class DataTableSorting(
    val sort: MonarchDataTableSort?,
    val onSortChange: ((MonarchDataTableSort) -> Unit)?,
)

/** The toolbar, the header row of the table layout and a row per record. */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongMethod")
private fun <T, K : Any> LazyListScope.dataTableItems(
    setup: DataTableSetup<T, K>,
    table: Boolean,
    weights: List<Float>,
) {
    val texts = setup.texts
    val sortable = setup.columns.filter { it.sortable }
    val onSortChange = setup.onSortChange
    val sortMenu = !table && onSortChange != null && sortable.isNotEmpty()
    val selector = setup.selector
    val toolbarSelect = selector != null && !table
    if (toolbarSelect || sortMenu) {
        item(key = "monarch-datatable-toolbar") {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = MonarchDataTableTokens.RowPaddingHorizontal),
                horizontalArrangement = Arrangement.SpaceBetween,
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (selector != null && toolbarSelect) {
                    MonarchDataTableSelectAll(
                        state = MonarchDataTableRules.selectAllState(selector.selection, selector.selectable),
                        selectedCount = selector.selection.size,
                        onClick = {
                            selector.onSelectionChange(
                                MonarchDataTableRules.toggledAll(selector.selection, selector.selectable),
                            )
                        },
                        texts = texts,
                    )
                }
                if (sortMenu) MonarchDataTableSortMenu(sortable, setup.sort, onSortChange, texts)
            }
        }
    }
    val hasActions = setup.rowActions != null
    if (table) {
        item(key = "monarch-datatable-header") {
            MonarchDataTableHeaderRow(
                columns = setup.columns,
                weights = weights,
                sort = setup.sort,
                onSortChange = onSortChange,
                selecting = selector != null,
                hasActions = hasActions,
                actionsIcon = setup.actionsIcon,
                texts = texts,
                selectAll = selector?.let { sel ->
                    {
                        MonarchDataTableSelectAll(
                            state = MonarchDataTableRules.selectAllState(sel.selection, sel.selectable),
                            selectedCount = sel.selection.size,
                            onClick = {
                                sel.onSelectionChange(MonarchDataTableRules.toggledAll(sel.selection, sel.selectable))
                            },
                            texts = texts,
                            labelled = false,
                        )
                    }
                },
            )
        }
    }
    val fields = if (setup.namedApart) setup.columns else setup.columns.drop(1)
    items(setup.rows, key = { setup.rowKey(it) }) { row ->
        val model = setup.model(row)
        if (table) {
            MonarchDataTableTableRow(
                row, setup.columns, weights, model, selector != null, hasActions, setup.actionsIcon, texts,
            )
        } else {
            MonarchDataTableRecordRow(row, fields, model, selector != null, setup.actionsIcon, texts)
        }
    }
}

/** What the table shows without rows: placeholder rows, the error state, or the empty state. */
@Composable
private fun NoRows(
    loading: Boolean,
    errorMessage: String?,
    filtered: Boolean,
    onRetry: (() -> Unit)?,
    onClearFilters: (() -> Unit)?,
    texts: MonarchDataTableTexts,
) {
    when {
        loading -> MonarchDataTableLoadingRows(texts.loading)
        errorMessage != null -> MonarchErrorState(
            title = texts.errorTitle,
            message = errorMessage,
            onRetry = onRetry,
        )
        filtered -> MonarchEmptyState(
            title = texts.filteredTitle,
            kind = MonarchEmptyStateKind.Filtered,
            message = texts.filteredMessage,
            onClearFilters = onClearFilters,
        )
        else -> MonarchEmptyState(title = texts.emptyTitle, message = texts.emptyMessage)
    }
}
