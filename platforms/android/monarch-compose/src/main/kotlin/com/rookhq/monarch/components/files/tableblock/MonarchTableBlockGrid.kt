package com.rookhq.monarch.components.files.tableblock

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/**
 * Lays out cells in rows and columns. Every column is as wide as its widest
 * cell between [minColumnWidth] and [maxColumnWidth], and every row as high
 * as its highest cell, so a cell fills its whole box and can draw its fill
 * and lines. [cell] must emit one layout per cell.
 */
@Composable
internal fun MonarchTableBlockGrid(
    rowCount: Int,
    columnCount: Int,
    minColumnWidth: Dp,
    maxColumnWidth: Dp,
    modifier: Modifier = Modifier,
    cell: @Composable (row: Int, column: Int) -> Unit,
) {
    Layout(
        content = {
            for (row in 0 until rowCount) {
                for (column in 0 until columnCount) cell(row, column)
            }
        },
        modifier = modifier,
    ) { measurables, _ ->
        val min = minColumnWidth.roundToPx()
        val max = maxOf(min, maxColumnWidth.roundToPx())
        fun at(row: Int, column: Int) = measurables[row * columnCount + column]
        val widths = IntArray(columnCount) { column ->
            (0 until rowCount).maxOf { row -> at(row, column).maxIntrinsicWidth(Constraints.Infinity) }
                .coerceIn(min, max)
        }
        val heights = IntArray(rowCount) { row ->
            (0 until columnCount).maxOf { column -> at(row, column).minIntrinsicHeight(widths[column]) }
        }
        val placeables = measurables.mapIndexed { index, measurable ->
            measurable.measure(Constraints.fixed(widths[index % columnCount], heights[index / columnCount]))
        }
        layout(widths.sum(), heights.sum()) {
            var y = 0
            for (row in 0 until rowCount) {
                var x = 0
                for (column in 0 until columnCount) {
                    placeables[row * columnCount + column].placeRelative(x, y)
                    x += widths[column]
                }
                y += heights[row]
            }
        }
    }
}
