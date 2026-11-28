package dev.monarchui.components.files.tableblock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.R
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * A small read-only table of text inside a document.
 *
 * A table wider than its container scrolls sideways instead of widening the
 * page. Columns are as wide as their text, between 96dp and 240dp at the
 * default font size, and the text wraps inside them. Header cells sit on the
 * subtle background and use the strong body style.
 *
 * Built by Monarch: Compose has no table component. TalkBack reads the table
 * as a collection named by [label], else [caption], else "Table"; each cell
 * carries its row and column, header cells are headings, and an empty cell is
 * read as "Empty".
 *
 * @param data the cells.
 * @param modifier the modifier of the block.
 * @param caption text above the table, already localised. Also the spoken name of the table.
 * @param label the spoken name of the table when the caption is not the right one.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchTableBlock(
    data: MonarchTableBlockData,
    modifier: Modifier = Modifier,
    caption: String? = null,
    label: String? = null,
    labels: MonarchTableBlockLabels = MonarchTableBlockLabels(),
) {
    val empty = monarchString(labels.emptyCell, R.string.monarch_tableblock_empty_cell)
    val scale = MonarchTableBlockTokens.widthScale(LocalDensity.current.fontScale)
    TableFrame(data, modifier, caption, label, labels, controls = null) {
        MonarchTableBlockGrid(
            rowCount = data.rowCount,
            columnCount = data.columnCount,
            minColumnWidth = MonarchTableBlockTokens.MinColumnWidth * scale,
            maxColumnWidth = MonarchTableBlockTokens.MaxColumnWidth * scale,
            modifier = it,
        ) { row, column ->
            val header = data.isHeader(row)
            val text = data[row, column]
            CellBox(data, MonarchTableBlockCell(row, column)) {
                Text(
                    text = text,
                    modifier = Modifier.semantics {
                        collectionItemInfo = CollectionItemInfo(row, 1, column, 1)
                        if (text.isBlank()) contentDescription = MonarchTableBlockTokens.cellLabel(text, empty)
                        if (header) heading()
                    },
                    style = if (header) MonarchTheme.typography.bodyStrong else MonarchTheme.typography.body,
                    color = MonarchTableBlockTokens.foreground(enabled = true, MonarchTheme.colors),
                )
            }
        }
    }
}

/**
 * A small editable table of text inside a document: every cell is a text
 * field, and a "Table" button above it opens a menu to add and delete rows
 * and columns, switch the header row and delete the table.
 *
 * The menu acts on the cell that last had focus, or on the last row and
 * column when none had; a new row or column takes focus. Every column is
 * 144dp wide at the default font size and every cell at least 48dp high.
 * The data is hoisted: each change calls [onDataChange] with a new value.
 *
 * @param data the cells.
 * @param onDataChange called with the new table after an edit, an added or deleted row or column, or a change
 * of the header row.
 * @param modifier the modifier of the block.
 * @param caption text above the table, already localised. Also the spoken name of the table.
 * @param label the spoken name of the table when the caption is not the right one.
 * @param enabled whether the cells and the menu respond.
 * @param labels replacements for the built-in strings.
 * @param onDelete adds "Delete table" to the menu and is called when it is chosen. The app removes the table.
 */
@Composable
public fun MonarchTableBlock(
    data: MonarchTableBlockData,
    onDataChange: (MonarchTableBlockData) -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    label: String? = null,
    enabled: Boolean = true,
    labels: MonarchTableBlockLabels = MonarchTableBlockLabels(),
    onDelete: (() -> Unit)? = null,
) {
    var active by remember { mutableStateOf<MonarchTableBlockCell?>(null) }
    var focusTarget by remember { mutableStateOf<MonarchTableBlockCell?>(null) }
    val requesters = remember { mutableMapOf<MonarchTableBlockCell, FocusRequester>() }
    val scale = MonarchTableBlockTokens.widthScale(LocalDensity.current.fontScale)
    val target = MonarchTableBlockTokens.target(active, data)
    val change = { next: MonarchTableBlockData, focus: MonarchTableBlockCell? ->
        if (next != data) {
            onDataChange(next)
            focus?.let {
                active = it
                focusTarget = it
            }
        }
    }
    LaunchedEffect(focusTarget) {
        focusTarget?.let { cell -> requesters[cell]?.requestFocus() }
        focusTarget = null
    }
    val controls: @Composable () -> Unit = {
        MonarchTableBlockMenu(
            data = data,
            target = target,
            enabled = enabled,
            labels = labels,
            onChange = change,
            onDelete = onDelete,
        )
    }
    TableFrame(data, modifier, caption, label, labels, controls) {
        MonarchTableBlockGrid(
            rowCount = data.rowCount,
            columnCount = data.columnCount,
            minColumnWidth = MonarchTableBlockTokens.EditableColumnWidth * scale,
            maxColumnWidth = MonarchTableBlockTokens.EditableColumnWidth * scale,
            modifier = it,
        ) { row, column ->
            val cell = MonarchTableBlockCell(row, column)
            val requester = remember(cell) { FocusRequester() }
            requesters[cell] = requester
            val position = labels.cellPosition?.invoke(row + 1, column + 1)
                ?: stringResource(R.string.monarch_tableblock_cell_position, row + 1, column + 1)
            val header = data.isHeader(row)
            CellBox(data, cell) {
                BasicTextField(
                    value = data[row, column],
                    onValueChange = { text -> change(data.withCell(row, column, text), null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MonarchTableBlockTokens.EditableCellMinHeight)
                        .focusRequester(requester)
                        .onFocusChanged { if (it.isFocused) active = cell }
                        .semantics {
                            contentDescription = position
                            collectionItemInfo = CollectionItemInfo(row, 1, column, 1)
                            if (header) heading()
                        },
                    enabled = enabled,
                    textStyle = (if (header) MonarchTheme.typography.bodyStrong else MonarchTheme.typography.body)
                        .copy(color = MonarchTableBlockTokens.foreground(enabled, MonarchTheme.colors)),
                    cursorBrush = SolidColor(MonarchTheme.colors.textDefault),
                    decorationBox = { field -> Box(contentAlignment = Alignment.CenterStart) { field() } },
                )
            }
        }
    }
}

/** The controls, the caption and the scrolling frame around a grid. */
@Composable
private fun TableFrame(
    data: MonarchTableBlockData,
    modifier: Modifier,
    caption: String?,
    label: String?,
    labels: MonarchTableBlockLabels,
    controls: (@Composable () -> Unit)?,
    grid: @Composable (Modifier) -> Unit,
) {
    val colors = MonarchTheme.colors
    val name = MonarchTableBlockTokens.regionLabel(
        label = label,
        caption = caption,
        fallback = monarchString(labels.region, R.string.monarch_tableblock_region),
    )
    val shape = MonarchShapeDefaults.Control
    Column(
        modifier = modifier.semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(MonarchTableBlockTokens.Gap),
    ) {
        controls?.invoke()
        if (!caption.isNullOrEmpty()) {
            Text(
                text = caption,
                modifier = if (MonarchTableBlockTokens.captionIsSpokenName(label)) {
                    Modifier.clearAndSetSemantics { }
                } else {
                    Modifier
                },
                style = MonarchTheme.typography.caption,
                color = MonarchTableBlockTokens.caption(colors),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = name
                    isTraversalGroup = true
                }
                .horizontalScroll(rememberScrollState()),
        ) {
            grid(
                Modifier
                    .clip(shape)
                    .border(MonarchShapeDefaults.BorderWidth, MonarchTableBlockTokens.line(colors), shape)
                    .semantics { collectionInfo = CollectionInfo(data.rowCount, data.columnCount) },
            )
        }
    }
}

/** One cell: its fill, the lines after it and below it, and its padding. */
@Composable
private fun CellBox(
    data: MonarchTableBlockData,
    cell: MonarchTableBlockCell,
    content: @Composable () -> Unit,
) {
    val colors = MonarchTheme.colors
    val line = MonarchTableBlockTokens.line(colors)
    val lineWidth = MonarchShapeDefaults.BorderWidth
    val lastRow = cell.row == data.rowCount - 1
    val lastColumn = cell.column == data.columnCount - 1
    Box(
        modifier = Modifier
            .background(MonarchTableBlockTokens.background(data.isHeader(cell.row), colors))
            .drawBehind { drawCellLines(line, lineWidth, lastRow, lastColumn) }
            .padding(
                horizontal = MonarchTableBlockTokens.CellHorizontalPadding,
                vertical = MonarchTableBlockTokens.CellVerticalPadding,
            ),
        contentAlignment = Alignment.TopStart,
    ) {
        content()
    }
}

private fun DrawScope.drawCellLines(
    color: Color,
    width: Dp,
    lastRow: Boolean,
    lastColumn: Boolean,
) {
    val stroke = width.toPx()
    if (!lastRow) {
        val y = size.height - stroke / 2
        drawLine(color, Offset(0f, y), Offset(size.width, y), stroke)
    }
    if (!lastColumn) {
        val x = if (layoutDirection == LayoutDirection.Rtl) stroke / 2 else size.width - stroke / 2
        drawLine(color, Offset(x, 0f), Offset(x, size.height), stroke)
    }
}
