package com.rookhq.monarch.components.files.columnsblock

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints

/**
 * Side-by-side columns inside a document, for example a "before" and an
 * "after", which stack into one column where the width is narrow.
 *
 * Built by Monarch with a Compose `Layout`: Material 3 has no columns block.
 * Columns are declared with [MonarchColumnsBlockScope.column]. Two columns
 * share the width at [ratio]; three or more are equal. The columns sit side
 * by side, top-aligned, from 480dp of available width at the default font
 * size; the threshold grows with the font size setting, so at large text the
 * columns stack in order. Each column is a TalkBack traversal group, so it is
 * read completely before the next. Columns are placed from the start edge.
 *
 * For a split the user resizes, use `MonarchResizablePanels`.
 *
 * @param modifier the modifier of the block.
 * @param ratio the share of the width for the first of two columns, kept between 0.2 and 0.8 (0.5 when
 * not a number). Ignored for any other number of columns.
 * @param content declares the columns with `column { }`.
 */
@Composable
public fun MonarchColumnsBlock(
    modifier: Modifier = Modifier,
    ratio: Float = MonarchColumnsBlockTokens.DEFAULT_RATIO,
    content: MonarchColumnsBlockScope.() -> Unit,
) {
    val columns = MonarchColumnsBlockScopeImpl().apply(content).columns.toList()
    val fontScale = LocalDensity.current.fontScale
    val tokens = MonarchColumnsBlockTokens
    Layout(
        modifier = modifier,
        content = {
            columns.forEach { column ->
                Column(modifier = Modifier.semantics { isTraversalGroup = true }, content = column)
            }
        },
    ) { measurables, constraints ->
        val total = if (constraints.hasBoundedWidth) constraints.maxWidth else tokens.StackBelow.roundToPx()
        val stacked = measurables.size <= 1 || tokens.stacks(total.toDp(), fontScale)
        if (stacked) {
            val gap = tokens.StackedGap.roundToPx()
            val child = Constraints(minWidth = total, maxWidth = total)
            val placeables = measurables.map { it.measure(child) }
            val height = placeables.sumOf { it.height } + gap * (placeables.size - 1).coerceAtLeast(0)
            layout(total, height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
                var y = 0
                placeables.forEach { placeable ->
                    placeable.placeRelative(0, y)
                    y += placeable.height + gap
                }
            }
        } else {
            val gap = tokens.ColumnGap.roundToPx()
            val widths = tokens.widths(measurables.size, total, gap, ratio)
            val placeables = measurables.mapIndexed { index, measurable ->
                measurable.measure(Constraints(minWidth = widths[index], maxWidth = widths[index]))
            }
            val height = placeables.maxOf { it.height }
            layout(total, height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
                var x = 0
                placeables.forEach { placeable ->
                    placeable.placeRelative(x, 0)
                    x += placeable.width + gap
                }
            }
        }
    }
}
