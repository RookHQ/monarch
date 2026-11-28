package dev.monarchui.components.intelligence.aitableanswer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.feedback.skeleton.MonarchSkeleton
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import dev.monarchui.components.typography.tabularnumberstyle.MonarchTabularNumberStyle
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/**
 * The grid of `MonarchAiTableAnswer`: a header row and one row per value
 * row, in a horizontally scrolling `Row` inside a bordered box. Each column
 * is as wide as its widest text, measured with a `TextMeasurer`, so every
 * row lines up and cells stay on one line.
 */
@Suppress("LongParameterList")
@Composable
internal fun MonarchAiTableAnswerGrid(
    rows: List<Map<String, MonarchAiJSON>>,
    columns: List<MonarchAiTableAnswerData.Column>,
    sort: MonarchAiTableAnswerData.Sort?,
    sortable: Boolean,
    streaming: Boolean,
    locale: Locale,
    labels: MonarchAiTableAnswerLabels,
    onSort: (key: String) -> Unit,
) {
    val colors = MonarchTheme.colors
    val natural = rememberColumnWidths(rows, columns, locale, sortable)
    val emptyLabel = monarchString(labels.empty, R.string.monarch_aitableanswer_empty)
    val shape = MonarchShapeDefaults.Container
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // A table that is narrower than its container shares the spare width between its columns, so it fills the
        // card; a wider one keeps its widths and scrolls.
        val naturalTotal = natural.fold(MonarchAiTableAnswerTokens.Zero) { sum, width -> sum + width }
        val spare = ((maxWidth - MonarchShapeDefaults.BorderWidth * 2 - naturalTotal) / natural.size.coerceAtLeast(1))
            .coerceAtLeast(MonarchAiTableAnswerTokens.Zero)
        val widths = natural.map { it + spare }
        val total = naturalTotal + spare * natural.size
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(MonarchShapeDefaults.BorderWidth, MonarchAiTableAnswerTokens.border(colors), shape)
                .horizontalScroll(rememberScrollState()),
        ) {
            Column {
                Row(modifier = Modifier.background(MonarchAiTableAnswerTokens.headerBackground(colors))) {
                    columns.forEachIndexed { index, column ->
                        HeaderCell(column, widths[index], sort, sortable, labels, onSort)
                    }
                }
                rows.forEach { row ->
                    MonarchDivider(Modifier.width(total), variant = MonarchDividerVariant.Subtle)
                    val spoken = MonarchAiTableAnswerData.rowLabel(row, columns, locale, emptyLabel)
                    Row(modifier = Modifier.clearAndSetSemantics { contentDescription = spoken }) {
                        columns.forEachIndexed { index, column ->
                            ValueCell(row[column.key], column, widths[index], locale)
                        }
                    }
                }
                if (streaming) {
                    MonarchDivider(Modifier.width(total), variant = MonarchDividerVariant.Subtle)
                    val loading = monarchString(labels.loadingRows, R.string.monarch_aitableanswer_loading_rows)
                    Row(modifier = Modifier.clearAndSetSemantics { contentDescription = loading }) {
                        widths.forEach { width ->
                            Box(modifier = Modifier.width(width).padding(MonarchAiTableAnswerTokens.CellPadding)) {
                                MonarchSkeleton(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = MonarchAiTableAnswerTokens.PlaceholderHeight),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun HeaderCell(
    column: MonarchAiTableAnswerData.Column,
    width: Dp,
    sort: MonarchAiTableAnswerData.Sort?,
    sortable: Boolean,
    labels: MonarchAiTableAnswerLabels,
    onSort: (key: String) -> Unit,
) {
    val colors = MonarchTheme.colors
    val direction = sort?.takeIf { it.key == column.key }?.ascending
    val state = direction?.let {
        labels.sortState?.invoke(it) ?: stringResource(
            if (it) R.string.monarch_aitableanswer_sort_ascending else R.string.monarch_aitableanswer_sort_descending,
        )
    }
    val interaction = if (sortable) {
        Modifier
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(indication = ripple(), interactionSource = null, role = Role.Button) { onSort(column.key) }
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .width(width)
            .heightIn(min = if (sortable) MonarchTouchTarget.Minimum else MonarchAiTableAnswerTokens.SortIconSize)
            .then(interaction)
            .semantics(mergeDescendants = true) {
                heading()
                if (state != null) stateDescription = state
            }
            .padding(MonarchAiTableAnswerTokens.CellPadding),
        horizontalArrangement = if (column.alignment == MonarchAiTableColumnAlignment.End) {
            Arrangement.spacedBy(MonarchAiTableAnswerTokens.SortIconGap, Alignment.End)
        } else {
            Arrangement.spacedBy(MonarchAiTableAnswerTokens.SortIconGap)
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = column.label,
            style = headerStyle(),
            color = MonarchAiTableAnswerTokens.headerColor(colors),
            maxLines = 1,
            softWrap = false,
        )
        // As on the web, the chevrons show on the sorted column only; the header stays a button for the rest.
        if (sortable && direction != null) SortChevrons(direction)
    }
}

@Composable
private fun ValueCell(value: MonarchAiJSON?, column: MonarchAiTableAnswerData.Column, width: Dp, locale: Locale) {
    val blank = MonarchAiTableAnswerValues.isBlank(value)
    Text(
        text = if (blank) {
            MonarchAiTableAnswerTokens.EMPTY_MARK
        } else {
            MonarchAiTableAnswerValues.display(value, column.type, locale)
        },
        modifier = Modifier
            .width(width)
            .padding(MonarchAiTableAnswerTokens.CellPadding),
        style = cellStyle(column.type),
        color = MonarchAiTableAnswerTokens.cellColor(blank, MonarchTheme.colors),
        textAlign = if (column.alignment == MonarchAiTableColumnAlignment.End) TextAlign.End else TextAlign.Start,
        maxLines = 1,
        softWrap = false,
    )
}

/** Two small triangles; the active direction is drawn strong. Decorative: the direction is the header's state. */
@Composable
private fun SortChevrons(ascending: Boolean?) {
    val colors = MonarchTheme.colors
    val up = MonarchAiTableAnswerTokens.chevronColor(ascending == true, colors)
    val down = MonarchAiTableAnswerTokens.chevronColor(ascending == false, colors)
    Canvas(modifier = Modifier.size(MonarchAiTableAnswerTokens.SortIconSize)) {
        val w = size.width
        val h = size.height
        val gap = h / GapDivisor
        drawPath(path = triangle(Offset(w / 2, 0f), w, h / 2 - gap, pointsUp = true), color = up)
        drawPath(path = triangle(Offset(w / 2, h), w, h / 2 - gap, pointsUp = false), color = down)
    }
}

private const val GapDivisor = 12f

private fun triangle(tip: Offset, width: Float, height: Float, pointsUp: Boolean): Path = Path().apply {
    val base = if (pointsUp) tip.y + height else tip.y - height
    moveTo(tip.x, tip.y)
    lineTo(tip.x - width / 2, base)
    lineTo(tip.x + width / 2, base)
    close()
}

@Composable
private fun headerStyle(): TextStyle = MonarchTheme.typography.label

@Composable
private fun cellStyle(type: MonarchAiTableColumnType): TextStyle {
    // The small body size, so a four-column answer fits a phone without a sideways scroll.
    val body = MonarchTheme.typography.body.copy(fontSize = MonarchTokens.FontSize.sm.sp)
    return if (type == MonarchAiTableColumnType.Number) MonarchTabularNumberStyle.applyTo(body) else body
}

/** The width of each column: its widest header or value, with the padding and the sort icon. */
@Composable
private fun rememberColumnWidths(
    rows: List<Map<String, MonarchAiJSON>>,
    columns: List<MonarchAiTableAnswerData.Column>,
    locale: Locale,
    sortable: Boolean,
): List<Dp> {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val header = headerStyle()
    val textStyle = cellStyle(MonarchAiTableColumnType.Text)
    val numberStyle = cellStyle(MonarchAiTableColumnType.Number)
    return remember(rows, columns, locale, sortable, density, header, textStyle) {
        columns.map { column ->
            val style = if (column.type == MonarchAiTableColumnType.Number) numberStyle else textStyle
            val headerWidth = measurer.measure(column.label, header, softWrap = false).size.width
            val cellWidth = rows.maxOfOrNull { row ->
                val value = row[column.key]
                val text = if (MonarchAiTableAnswerValues.isBlank(value)) {
                    MonarchAiTableAnswerTokens.EMPTY_MARK
                } else {
                    MonarchAiTableAnswerValues.display(value, column.type, locale)
                }
                measurer.measure(text, style, softWrap = false).size.width
            } ?: 0
            with(density) {
                // The sort chevrons show on the sorted column only and take the column's spare width.
                val content = maxOf(headerWidth.toDp(), cellWidth.toDp())
                val width = content + MonarchAiTableAnswerTokens.CellHorizontalPadding * 2 + 1.toDp()
                if (sortable) maxOf(width, MonarchTouchTarget.Minimum) else width
            }
        }
    }
}
