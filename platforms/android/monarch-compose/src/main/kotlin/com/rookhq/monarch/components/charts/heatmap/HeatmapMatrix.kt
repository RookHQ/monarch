package com.rookhq.monarch.components.charts.heatmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme
import kotlin.math.ceil

/** What the matrix draws besides the values: the colour, the values switch and the selected cell. */
internal class HeatmapMatrixStyle(
    val slot: MonarchChartColor,
    val showValues: Boolean,
    val selectedRow: Int?,
    val selectedColumn: Int?,
)

/**
 * The matrix of a heatmap: column labels on top, row labels at the start and
 * a row of square cells for each row. The cells are drawn on a `Canvas`,
 * mirrored by hand right to left, with one invisible element per cell on top
 * that TalkBack reads row by row. The labels are hidden from TalkBack, as each
 * cell names its row and column.
 */
@Composable
internal fun HeatmapMatrix(
    model: HeatmapModel,
    words: HeatmapWords,
    style: HeatmapMatrixStyle,
    onTap: (row: Int, column: Int) -> Unit,
    selectLabel: String,
    onSelect: ((row: Int, column: Int) -> Unit)?,
) {
    val colors = MonarchTheme.colors
    val caption = MonarchTheme.typography.caption
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val width = constraints.maxWidth.toFloat()
        val gap = with(density) { MonarchHeatmapTokens.Gap.toPx() }
        val labelGap = with(density) { MonarchHeatmapTokens.LabelGap.toPx() }
        val rowLabelWidth = ceil(
            minOf(
                model.rows.maxOfOrNull { measurer.measure(it.label, caption).size.width }?.toFloat() ?: 0f,
                width * MonarchHeatmapTokens.ROW_LABEL_SHARE,
            ),
        )
        val step = HeatmapLayout.step(
            available = width - rowLabelWidth - labelGap,
            columns = model.columns.size,
            minStep = with(density) { MonarchHeatmapTokens.MinStep.toPx() },
            maxStep = with(density) { MonarchHeatmapTokens.MaxCell.toPx() } + gap,
        )
        val cell = maxOf(1f, step - gap)
        val columnLabelWidth = model.columns.maxOfOrNull { measurer.measure(it.label, caption).size.width } ?: 0
        val every = HeatmapLayout.labelEvery(step, columnLabelWidth + labelGap)
        val geometry = HeatmapGeometry(step = step, cell = cell, columns = model.columns.size)
        Column(verticalArrangement = Arrangement.spacedBy(MonarchHeatmapTokens.Gap)) {
            Row {
                Spacer(Modifier.width(with(density) { (rowLabelWidth + labelGap).toDp() }))
                HeatmapColumnLabels(model, geometry, every, caption, colors)
            }
            model.rows.forEachIndexed { row, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.label,
                        modifier = Modifier
                            .width(with(density) { rowLabelWidth.toDp() })
                            .clearAndSetSemantics { },
                        style = caption,
                        color = MonarchHeatmapTokens.labelColor(colors),
                        textAlign = TextAlign.End,
                    )
                    Spacer(Modifier.width(MonarchHeatmapTokens.LabelGap))
                    HeatmapRow(model, row, words, style, geometry, measurer, onTap, selectLabel, onSelect)
                }
            }
        }
    }
}

/** The sizes of the cells, in pixels. */
internal class HeatmapGeometry(val step: Float, val cell: Float, val columns: Int)

/** The column labels, each centred over its column; only every [every]th label, so they never overlap. */
@Composable
private fun HeatmapColumnLabels(
    model: HeatmapModel,
    geometry: HeatmapGeometry,
    every: Int,
    style: TextStyle,
    colors: MonarchColors,
) {
    val width = geometry.step * geometry.columns
    Layout(
        content = {
            model.columns.forEach {
                Text(
                    text = it.label,
                    modifier = Modifier.clearAndSetSemantics { },
                    style = style,
                    color = MonarchHeatmapTokens.labelColor(colors),
                    maxLines = 1,
                )
            }
        },
    ) { measurables, _ ->
        val placeables = measurables.map { it.measure(Constraints()) }
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(ceil(width).toInt(), height) {
            placeables.forEachIndexed { index, placeable ->
                if (index % every != 0) return@forEachIndexed
                val centre = index * geometry.step + geometry.cell / 2
                val x = (centre - placeable.width / 2f).coerceIn(0f, maxOf(0f, width - placeable.width))
                // placeRelative mirrors the position right to left, like the cells.
                placeable.placeRelative(x.toInt(), 0)
            }
        }
    }
}

/** One row of cells: drawn on a canvas, with an invisible TalkBack element per cell and taps by position. */
@Composable
@Suppress("LongParameterList")
private fun HeatmapRow(
    model: HeatmapModel,
    row: Int,
    words: HeatmapWords,
    style: HeatmapMatrixStyle,
    geometry: HeatmapGeometry,
    measurer: TextMeasurer,
    onTap: (row: Int, column: Int) -> Unit,
    selectLabel: String,
    onSelect: ((row: Int, column: Int) -> Unit)?,
) {
    val colors = MonarchTheme.colors
    val caption = MonarchTheme.typography.caption
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val tap by rememberUpdatedState(onTap)
    val width = with(density) { (geometry.step * geometry.columns).toDp() }
    val cellSize = with(density) { geometry.cell.toDp() }
    Box(
        modifier = Modifier
            .size(width, cellSize)
            .pointerInput(row, geometry.step, geometry.columns, rtl) {
                detectTapGestures { offset ->
                    HeatmapLayout.columnAt(offset.x, geometry.step, geometry.columns, rtl)
                        ?.let { tap(row, it) }
                }
            },
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            for (column in 0 until geometry.columns) {
                drawHeatmapCell(model, row, column, style, geometry, rtl, colors) { cell, left ->
                    if (style.showValues) drawHeatmapValue(cell, left, geometry, measurer, caption, colors, words)
                }
            }
        }
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.spacedBy(MonarchHeatmapTokens.Gap),
        ) {
            for (column in 0 until geometry.columns) {
                val spoken = HeatmapText.describe(model, row, column, words)
                val isSelected = row == style.selectedRow && column == style.selectedColumn
                Box(
                    Modifier
                        .size(cellSize)
                        .semantics {
                            contentDescription = spoken
                            if (isSelected) selected = true
                            collectionItemInfo = CollectionItemInfo(row, 1, column, 1)
                            if (onSelect != null) {
                                customActions = listOf(
                                    CustomAccessibilityAction(selectLabel) {
                                        onSelect(row, column)
                                        true
                                    },
                                )
                            }
                        },
                )
            }
        }
    }
}

/** Draws one cell: its level's fill (or the outline of no data) and the outline when it is selected. */
@Suppress("LongParameterList")
private inline fun DrawScope.drawHeatmapCell(
    model: HeatmapModel,
    row: Int,
    column: Int,
    style: HeatmapMatrixStyle,
    geometry: HeatmapGeometry,
    rtl: Boolean,
    colors: MonarchColors,
    content: DrawScope.(cell: HeatmapDrawnCell, left: Float) -> Unit,
) {
    val value = model.valueAt(row, column)
    val level = model.scale.level(value)
    val left = HeatmapLayout.cellLeft(column, geometry.step, geometry.cell, geometry.columns, rtl)
    val radius = CornerRadius(
        minOf(MonarchHeatmapTokens.CellRadius.toPx(), geometry.cell * MonarchHeatmapTokens.RADIUS_SHARE),
    )
    val side = Size(geometry.cell, geometry.cell)
    if (level == null) {
        val stroke = MonarchHeatmapTokens.EmptyStroke.toPx()
        drawRoundRect(
            color = MonarchHeatmapTokens.emptyOutline(colors),
            topLeft = Offset(left + stroke / 2, stroke / 2),
            size = Size(side.width - stroke, side.height - stroke),
            cornerRadius = radius,
            style = Stroke(stroke),
        )
    } else {
        drawRoundRect(
            color = MonarchHeatmapTokens.levelColor(level, style.slot, colors),
            topLeft = Offset(left, 0f),
            size = side,
            cornerRadius = radius,
        )
    }
    content(HeatmapDrawnCell(model.cellAt(row, column), level), left)
    if (row == style.selectedRow && column == style.selectedColumn) {
        val stroke = MonarchHeatmapTokens.SelectedStroke.toPx()
        drawRoundRect(
            color = MonarchHeatmapTokens.selectedOutline(colors),
            topLeft = Offset(left + stroke / 2, stroke / 2),
            size = Size(side.width - stroke, side.height - stroke),
            cornerRadius = radius,
            style = Stroke(stroke),
        )
    }
}

/** A cell and its level, as drawn. */
internal class HeatmapDrawnCell(val cell: MonarchHeatmapCell, val level: Int?)

/** Writes a value in its cell when it fits, on a surface chip on the two strongest levels. */
@Suppress("LongParameterList")
private fun DrawScope.drawHeatmapValue(
    drawn: HeatmapDrawnCell,
    left: Float,
    geometry: HeatmapGeometry,
    measurer: TextMeasurer,
    style: TextStyle,
    colors: MonarchColors,
    words: HeatmapWords,
) {
    val level = drawn.level ?: return
    val value = drawn.cell.value ?: return
    val text = drawn.cell.valueLabel ?: words.format(value)
    val layout = measurer.measure(text, style, maxLines = 1)
    val inset = MonarchHeatmapTokens.ChipInset.toPx()
    if (layout.size.width > geometry.cell - inset * 2 || layout.size.height > geometry.cell) return
    val top = (geometry.cell - layout.size.height) / 2
    if (level >= HeatmapLevels.CHIP) {
        drawRoundRect(
            color = MonarchHeatmapTokens.chipColor(colors),
            topLeft = Offset(left + inset, top),
            size = Size(geometry.cell - inset * 2, layout.size.height.toFloat()),
            cornerRadius = CornerRadius(MonarchHeatmapTokens.CellRadius.toPx()),
        )
    }
    drawText(
        textLayoutResult = layout,
        color = MonarchHeatmapTokens.valueColor(colors),
        topLeft = Offset(left + (geometry.cell - layout.size.width) / 2, top),
    )
}
