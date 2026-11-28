package com.rookhq.monarch.components.intelligence.aimarkdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import com.rookhq.monarch.components.intelligence.aimarkdown.MonarchAiMarkdownInline as Inline
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * A Markdown table as a grid that scrolls sideways inside its own border, so
 * a wide table never widens the answer. Columns take the width of their
 * widest cell up to [MonarchAiMarkdownTokens.MaxCellWidth], then wrap.
 * TalkBack gets the table's size and each cell's row and column.
 */
@Composable
internal fun MonarchAiMarkdownTable(
    table: MonarchAiMarkdownBlock.Table,
    annotate: (List<Inline>) -> AnnotatedString,
    body: TextStyle,
    colors: MonarchAiMarkdownColors,
    label: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MonarchAiMarkdownTokens
    val shape = MonarchTheme.shapes.md
    val columns = table.align.size
    val rows = listOf(table.header) + table.rows
    val header = body.copy(fontWeight = FontWeight(MonarchTokens.FontWeight.semibold))
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    // A table narrower than the answer fills it, as on the web; a wider one scrolls sideways.
    val fill = maxWidth - tokens.BorderWidth * 2
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(tokens.BorderWidth, colors.tableBorder, shape)
            .semantics { contentDescription = label }
            .horizontalScroll(rememberScrollState()),
    ) {
        Layout(
            content = {
                rows.forEachIndexed { r, row ->
                    for (c in 0 until columns) {
                        Cell(
                            text = annotate(row.getOrElse(c) { emptyList() }),
                            style = if (r == 0) header else body,
                            align = table.align[c],
                            background = if (r == 0) colors.tableHeader else Color.Transparent,
                            line = if (r < rows.lastIndex) colors.tableBorder else Color.Transparent,
                            modifier = Modifier.semantics {
                                collectionItemInfo = CollectionItemInfo(r, 1, c, 1)
                            },
                        )
                    }
                }
            },
            modifier = Modifier
                .widthIn(min = fill)
                .semantics { collectionInfo = CollectionInfo(rows.size, columns) },
        ) { measurables, constraints ->
            val maxCell = tokens.MaxCellWidth.roundToPx()
            val widths = IntArray(columns) { c ->
                rows.indices.maxOf { r -> measurables[r * columns + c].maxIntrinsicWidth(Constraints.Infinity) }
                    .coerceAtMost(maxCell)
            }
            // Spare width goes to the columns by their share, so the table fills the answer.
            val spare = (constraints.minWidth - widths.sum()).coerceAtLeast(0)
            if (spare > 0) {
                val total = widths.sum().coerceAtLeast(1)
                var given = 0
                for (c in 0 until columns) {
                    val extra = if (c == columns - 1) spare - given else spare * widths[c] / total
                    given += extra
                    widths[c] += extra
                }
            }
            val heights = IntArray(rows.size) { r ->
                (0 until columns).maxOf { c -> measurables[r * columns + c].minIntrinsicHeight(widths[c]) }
            }
            val placeables = measurables.mapIndexed { index, measurable ->
                measurable.measure(Constraints.fixed(widths[index % columns], heights[index / columns]))
            }
            layout(widths.sum(), heights.sum()) {
                var y = 0
                for (r in rows.indices) {
                    var x = 0
                    for (c in 0 until columns) {
                        placeables[r * columns + c].placeRelative(x, y)
                        x += widths[c]
                    }
                    y += heights[r]
                }
            }
        }
    }
    }
}

@Composable
private fun Cell(
    text: AnnotatedString,
    style: TextStyle,
    align: MonarchAiMarkdownAlign?,
    background: Color,
    line: Color,
    modifier: Modifier,
) {
    val tokens = MonarchAiMarkdownTokens
    Text(
        text = text,
        modifier = modifier
            .background(background)
            .drawBehind {
                val width = tokens.BorderWidth.toPx()
                val y = size.height - width / 2
                drawLine(line, Offset(0f, y), Offset(size.width, y), width)
            }
            .padding(horizontal = tokens.CellHorizontalPadding, vertical = tokens.CellVerticalPadding),
        style = style.copy(
            textAlign = when (align) {
                MonarchAiMarkdownAlign.Center -> TextAlign.Center
                MonarchAiMarkdownAlign.Right -> TextAlign.End
                MonarchAiMarkdownAlign.Left, null -> TextAlign.Start
            },
        ),
    )
}
