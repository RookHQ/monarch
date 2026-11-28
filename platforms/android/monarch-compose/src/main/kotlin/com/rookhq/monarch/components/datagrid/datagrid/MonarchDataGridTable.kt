package com.rookhq.monarch.components.datagrid.datagrid

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgress
import com.rookhq.monarch.components.datagrid.datatable.MonarchDataTableRowAction
import com.rookhq.monarch.components.forms.checkbox.MonarchCheckbox
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The grid layout: a header row, then the rows in a `LazyColumn`. Start-pinned columns, a horizontally
 * scrolling band of the other columns and end-pinned columns sit side by side in every row; one shared
 * `ScrollState` keeps the band of every row and the header at the same offset.
 */
@Composable
internal fun <T, K : Any> MonarchDataGridTable(
    setup: MonarchDataGridSetup<T, K>,
    state: LazyListState,
    loading: Boolean,
) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxWidth()) {
        if (loading) MonarchProgress(modifier = Modifier.fillMaxWidth(), label = setup.texts.loading)
        DataGridHeader(setup, scroll)
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false), state = state) {
            items(setup.rows, key = { setup.rowKey(it) }) { row -> DataGridRow(setup, row, scroll) }
        }
    }
}

/** The frame every grid line shares: checkbox slot, pinned cells, the scrolling band and the actions slot. */
@Composable
private fun <T, K : Any> DataGridLine(
    setup: MonarchDataGridSetup<T, K>,
    scroll: ScrollState,
    fill: Color,
    modifier: Modifier,
    select: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    cell: @Composable (MonarchDataGridColumn<T>) -> Unit,
) {
    val arrangement = setup.arrangement
    val edge = MonarchDataGridTokens.pinnedEdgeColor(MonarchTheme.colors)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().background(fill),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (setup.selector != null) UtilityCell { select() }
            if (arrangement.start.isNotEmpty()) {
                Row(
                    modifier = Modifier.pinnedEdge(edge, atEnd = true),
                    verticalAlignment = Alignment.CenterVertically,
                ) { arrangement.start.forEach { cell(it) } }
            }
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(scroll),
                verticalAlignment = Alignment.CenterVertically,
            ) { arrangement.middle.forEach { cell(it) } }
            if (arrangement.end.isNotEmpty()) {
                Row(
                    modifier = Modifier.pinnedEdge(edge, atEnd = false),
                    verticalAlignment = Alignment.CenterVertically,
                ) { arrangement.end.forEach { cell(it) } }
            }
            if (setup.hasActions) ActionsCell(setup) { actions() }
        }
        MonarchDivider(variant = MonarchDividerVariant.Subtle)
    }
}

/** A line at the inner edge of a pinned group: its end edge for start pins, its start edge for end pins. */
private fun Modifier.pinnedEdge(color: Color, atEnd: Boolean): Modifier = drawWithContent {
    drawContent()
    val ltr = layoutDirection == LayoutDirection.Ltr
    val x = if (atEnd == ltr) size.width - 1f else 0f
    drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = MonarchDataGridTokens.DividerWidth.toPx())
}

@Composable
private fun UtilityCell(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.width(MonarchDataGridTokens.UtilityWidth).heightIn(min = MonarchTouchTarget.Minimum),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun <T, K : Any> ActionsCell(setup: MonarchDataGridSetup<T, K>, content: @Composable () -> Unit) {
    val width = if (setup.behavior.actionsIcon != null) {
        MonarchDataGridTokens.UtilityWidth
    } else {
        MonarchDataGridTokens.ActionsTextWidth
    }
    Box(
        modifier = Modifier.width(width).heightIn(min = MonarchTouchTarget.Minimum),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** The header row: the select-all checkbox, a header per column and an empty actions slot. */
@Composable
private fun <T, K : Any> DataGridHeader(setup: MonarchDataGridSetup<T, K>, scroll: ScrollState) {
    val colors = MonarchTheme.colors
    DataGridLine(
        setup = setup,
        scroll = scroll,
        fill = MonarchDataGridTokens.headerColor(colors),
        modifier = Modifier,
        select = {
            val selector = setup.selector
            if (selector != null) {
                MonarchCheckbox(
                    state = selector.allState,
                    onClick = { selector.onSelectionChange(selector.toggledAll()) },
                    modifier = Modifier.semantics { contentDescription = setup.texts.selectAll },
                )
            }
        },
        actions = { },
    ) { column -> DataGridHeaderCell(setup, column) }
}

@Composable
private fun <T, K : Any> DataGridHeaderCell(setup: MonarchDataGridSetup<T, K>, column: MonarchDataGridColumn<T>) {
    val texts = setup.texts
    val items = setup.menuItems(column)
    val direction = setup.directionOf(column)
    val pin = setup.pinOf(column)
    val color = MonarchDataGridTokens.headerText(MonarchTheme.colors)
    var open by remember { mutableStateOf(false) }
    val interaction = if (items.isNotEmpty()) {
        Modifier
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(
                interactionSource = null,
                indication = ripple(),
                onClickLabel = texts.columnOptions,
                role = Role.Button,
                onClick = { open = true },
            )
    } else {
        Modifier
    }
    Box(modifier = Modifier.width(setup.widthOf(column))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MonarchTouchTarget.Minimum)
                .then(interaction)
                .semantics(mergeDescendants = true) {
                    heading()
                    val state = texts.headerState(column.sortable && setup.sorting.onSortChange != null, direction, pin)
                    if (state.isNotEmpty()) stateDescription = state
                }
                .padding(horizontal = MonarchDataGridTokens.CellPaddingHorizontal),
            horizontalArrangement = if (column.numeric) {
                Arrangement.End
            } else {
                Arrangement.Start
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(text = column.title, style = MonarchTextStyle.Label, color = color)
            if (direction != null) {
                MonarchText(
                    text = texts.mark(direction),
                    modifier = Modifier.clearAndSetSemantics { },
                    style = MonarchTextStyle.Label,
                    color = color,
                )
            }
        }
        MonarchMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { item ->
                MonarchMenuItem(
                    text = setup.labelOf(item),
                    onClick = {
                        open = false
                        setup.choose(column, item)
                    },
                )
            }
        }
    }
}

/** One row: its checkbox, a cell per column and its actions button. */
@Composable
private fun <T, K : Any> DataGridRow(setup: MonarchDataGridSetup<T, K>, row: T, scroll: ScrollState) {
    val name = setup.name(row)
    val selected = setup.selected(row)
    val actions = setup.behavior.rowActions?.invoke(row).orEmpty()
    val firstColumn = setup.arrangement.all.firstOrNull()
    DataGridLine(
        setup = setup,
        scroll = scroll,
        fill = MonarchDataGridTokens.rowColor(selected == true, MonarchTheme.colors),
        modifier = Modifier.onKeyEvent { event ->
            val toggle = selected != null && event.type == KeyEventType.KeyDown && event.key == Key.Spacebar
            if (toggle) setup.select(row, selected != true)
            toggle
        },
        select = {
            if (selected != null) {
                MonarchCheckbox(
                    checked = selected,
                    onCheckedChange = { setup.select(row, it) },
                    modifier = Modifier.semantics { contentDescription = setup.texts.selectRow(name) },
                )
            }
        },
        actions = { MonarchDataGridRowActions(name, actions, setup.behavior.actionsIcon, setup.texts) },
    ) { column ->
        val width = setup.widthOf(column)
        val edit = column.edit
        if (edit != null && edit.editable(row)) {
            Box(modifier = Modifier.width(width).padding(horizontal = MonarchDataGridTokens.LineGap)) {
                edit.Cell(
                    row = row,
                    label = setup.texts.editLabel(column.title, name),
                    display = column.value(row),
                    modifier = Modifier.fillMaxWidth(),
                    startEditing = false,
                    labels = setup.texts.editableCell,
                )
            }
        } else {
            DataGridReadOnlyCell(setup, row, column, width, strong = column == firstColumn, actions = actions)
        }
    }
}

/**
 * A read-only cell: focusable for hardware keyboards, read as "Column: value", a button that opens the row with
 * `onRowOpen`, and carrying the row's actions as TalkBack actions.
 */
@Composable
@Suppress("LongParameterList")
private fun <T, K : Any> DataGridReadOnlyCell(
    setup: MonarchDataGridSetup<T, K>,
    row: T,
    column: MonarchDataGridColumn<T>,
    width: Dp,
    strong: Boolean,
    actions: List<MonarchDataTableRowAction>,
) {
    val texts = setup.texts
    val colors = MonarchTheme.colors
    val text = column.value(row)
    val open = setup.behavior.onRowOpen
    val interaction = if (open != null) {
        Modifier.clickable(
            interactionSource = null,
            indication = ripple(),
            onClickLabel = texts.open,
            role = Role.Button,
            onClick = { open(row) },
        )
    } else {
        Modifier.focusable()
    }
    val style = MonarchTheme.typography.style(if (strong) MonarchTextStyle.BodyStrong else MonarchTextStyle.Body)
    Box(
        modifier = Modifier
            .width(width)
            .heightIn(min = MonarchTouchTarget.Minimum)
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .then(interaction)
            .clearAndSetSemantics {
                contentDescription = texts.cellDescription(column.title, text)
                val custom = monarchDataGridCustomActions(actions)
                if (custom.isNotEmpty()) customActions = custom
            },
        contentAlignment = if (column.numeric) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(
            text = text.ifBlank { texts.emptyCell },
            modifier = Modifier.padding(
                horizontal = MonarchDataGridTokens.CellPaddingHorizontal,
                vertical = MonarchDataGridTokens.cellPadding(setup.density),
            ),
            color = when {
                text.isBlank() -> MonarchDataGridTokens.headerText(colors)
                strong && open != null -> MonarchDataGridTokens.openableText(colors)
                else -> colors.textDefault
            },
            textAlign = if (column.numeric) TextAlign.End else TextAlign.Start,
            style = if (column.numeric) {
                style.copy(fontFeatureSettings = MonarchDataGridTokens.TABULAR_FIGURES)
            } else {
                style
            },
        )
    }
}
