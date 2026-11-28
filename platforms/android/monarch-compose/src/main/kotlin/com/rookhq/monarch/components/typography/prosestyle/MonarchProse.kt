package com.rookhq.monarch.components.typography.prosestyle

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A column of long-form content in [style]: headings, paragraphs, list
 * items, quotations, code blocks, captions and rules, spaced with the web
 * Prose rhythm (1.25 base sizes between blocks, 1.75 above a heading, 0.5
 * after it, 0.375 between list items).
 *
 * Built by Monarch from Compose `Column` and Material 3 `Text`; each block is
 * its own `Text`, so TalkBack moves through the content block by block and
 * headings have heading semantics. The column is not lazy and does not
 * scroll; put it in a scrolling container or a `LazyColumn` item.
 *
 * ```
 * MonarchProse {
 *     heading("Release notes", level = 1)
 *     paragraph("Invoices now show the tax number.")
 *     listItem("Faster search")
 * }
 * ```
 *
 * @param modifier the modifier of the column.
 * @param style the base size and whether the column keeps to the reading measure.
 * @param content adds the blocks in reading order.
 */
@Composable
public fun MonarchProse(
    modifier: Modifier = Modifier,
    style: MonarchProseStyle = MonarchProseStyle(),
    content: MonarchProseScope.() -> Unit,
) {
    val tokens = MonarchProseStyleTokens
    val blocks = ProseBlocks().apply(content).blocks
    val base = tokens.baseSize(style.size)
    val density = LocalDensity.current
    val em: (Float) -> Dp = { factor -> with(density) { (base * factor).sp.toDp() } }
    val measure = if (style.limitLineLength) Modifier.widthIn(max = em(tokens.MEASURE_EM)) else Modifier
    Column(modifier = modifier.then(measure)) {
        blocks.forEachIndexed { index, block ->
            val space = Modifier.padding(top = em(tokens.spaceBefore(block, blocks.getOrNull(index - 1))))
            ProseBlockView(block, style, em, space)
        }
    }
}

@Composable
private fun ProseBlockView(block: ProseBlock, style: MonarchProseStyle, em: (Float) -> Dp, modifier: Modifier) {
    val tokens = MonarchProseStyleTokens
    val typography = MonarchTheme.typography
    val colors = MonarchTheme.colors
    val body = tokens.body(style.size, typography, colors)
    val decorations = tokens.decorations(colors)
    when (block) {
        is ProseBlock.Heading -> Text(
            text = block.text,
            modifier = modifier.semantics { heading() },
            style = tokens.heading(block.level, style.size, typography, colors),
        )
        is ProseBlock.Paragraph -> Text(text = block.text, modifier = modifier, style = body)
        is ProseBlock.ListItem -> ListItem(block, body, decorations, em, modifier)
        is ProseBlock.Quote -> Text(
            text = block.text,
            modifier = modifier
                .drawBehind {
                    val width = tokens.QuoteBarWidth.toPx()
                    val x = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
                    drawRect(decorations.quoteBar, Offset(x, 0f), Size(width, size.height))
                }
                .padding(start = em(tokens.QUOTE_INDENT_EM)),
            style = body.copy(color = colors.textMuted),
        )
        is ProseBlock.Code -> CodeBlock(block.text, tokens.code(style.size, typography, colors), decorations, modifier)
        is ProseBlock.Caption -> Text(
            text = block.text,
            modifier = modifier,
            style = tokens.caption(style.size, typography, colors),
        )
        is ProseBlock.Table -> ProseTable(block, body, modifier)
        ProseBlock.Rule -> MonarchDivider(
            modifier = modifier.clearAndSetSemantics {},
            variant = MonarchDividerVariant.Subtle,
        )
    }
}

@Composable
private fun ListItem(
    item: ProseBlock.ListItem,
    body: TextStyle,
    decorations: ProseDecorationColors,
    em: (Float) -> Dp,
    modifier: Modifier,
) {
    val tokens = MonarchProseStyleTokens
    val numbered = item.marker != null
    Row(
        modifier = modifier
            .padding(start = em(tokens.LIST_INDENT_EM * item.depth))
            .then(if (numbered) Modifier.semantics(mergeDescendants = true) {} else Modifier),
    ) {
        Text(
            text = item.marker ?: tokens.bullet(item.depth),
            // A bullet is decoration; a number or letter is read before the item.
            modifier = Modifier
                .width(em(tokens.LIST_INDENT_EM))
                .then(if (numbered) Modifier else Modifier.clearAndSetSemantics {}),
            style = body.copy(color = decorations.marker),
        )
        Text(text = item.text, style = body)
    }
}

@Composable
private fun CodeBlock(code: String, style: TextStyle, decorations: ProseDecorationColors, modifier: Modifier) {
    val tokens = MonarchProseStyleTokens
    val shape = MonarchTheme.shapes.md
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(decorations.codeBackground, shape)
            .border(MonarchShapeDefaults.BorderWidth, decorations.codeBorder, shape),
    ) {
        // Code reads left to right in every layout direction.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                text = code,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = tokens.CodeHorizontalPadding, vertical = tokens.CodeVerticalPadding),
                style = style,
                softWrap = false,
            )
        }
    }
}

/** The header row in the body weight over the rows, each with a hairline under it, in equal columns. */
@Composable
private fun ProseTable(table: ProseBlock.Table, body: TextStyle, modifier: Modifier) {
    val colors = MonarchTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        TableRow(table.columns, body.copy(fontWeight = FontWeight.SemiBold), colors.borderDefault)
        table.rows.forEach { row -> TableRow(row, body, colors.borderSubtle) }
    }
}

@Composable
private fun TableRow(cells: List<String>, style: TextStyle, line: Color) {
    val tokens = MonarchProseStyleTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val width = tokens.TableLineWidth.toPx()
                drawRect(line, Offset(0f, size.height - width), Size(size.width, width))
            }
            .padding(vertical = tokens.TableCellVerticalPadding),
    ) {
        cells.forEach { cell ->
            Text(text = cell, modifier = Modifier.weight(1f).padding(end = tokens.TableCellEndPadding), style = style)
        }
    }
}
