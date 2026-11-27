package com.rookhq.monarch.components.datagrid.datatable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.tokens.MonarchTokens
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchTheme

/** The weight of each column of the table layout and the width the table needs for them. */
internal class DataTableWidths(val weights: List<Float>, val needed: Dp)

/** A header cell also holds the sort chevron or mark beside its title. */
private val HEADER_EXTRA = MonarchTokens.Space.space6.dp
private val None = Dp(0f)

private const val SampleRows = 200

/** Room beyond the measured word, so a rounding difference never breaks its last letter. */
private val Slack = MonarchTokens.Space.space1.dp

/**
 * Shares [available] between the columns by weight, but never gives a column less than its longest word
 * (a value or the title), so a word is not broken in the middle. When the words of all columns do not fit,
 * [needed] grows past [available] and the table scrolls sideways.
 */
@Composable
internal fun <T> rememberDataTableWidths(
    columns: List<MonarchDataTableColumn<T>>,
    rows: List<T>,
    available: Dp,
    selecting: Boolean,
    hasActions: Boolean,
    floor: Dp,
): DataTableWidths {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val typography = MonarchTheme.typography
    val gap = MonarchDataTableTokens.CellGap
    val fixed = MonarchDataTableTokens.FixedColumnWidth
    return remember(columns, rows, available, selecting, hasActions, floor, density, typography) {
        fun word(text: String, style: androidx.compose.ui.text.TextStyle): Dp {
            val widest = text.split(' ', '\n').maxOfOrNull { measurer.measure(it, style).size.width } ?: 0
            return with(density) { widest.toDp() }
        }
        val label = typography.style(MonarchTextStyle.Label)
        val sample = rows.take(SampleRows)
        val minimums = columns.mapIndexed { index, column ->
            val body = typography.style(if (index == 0) MonarchTextStyle.BodyStrong else MonarchTextStyle.Body)
            val header = word(column.title, label) + if (column.sortable) HEADER_EXTRA else None
            val cells = if (column.cell != null) None else sample.maxOfOrNull { word(column.value(it), body) } ?: None
            maxOf(header, cells, floor) + Slack
        }
        val fixedCount = listOf(selecting, hasActions).count { it }
        val overhead = MonarchDataTableTokens.RowPaddingHorizontal * 2 + (fixed + gap) * fixedCount +
            gap * (columns.size - 1).coerceAtLeast(0)
        val free = (available - overhead).value
        val widths = resolveWidths(columns.map { it.weight }, minimums.map { it.value }, free)
        val sum = widths.sum()
        DataTableWidths(widths, maxOf(available, (sum + overhead.value).dp))
    }
}

/** The width of each column: by weight out of [free], but no column under its minimum; the rest share what is left. */
private fun resolveWidths(weights: List<Float>, minimums: List<Float>, free: Float): List<Float> {
    val pinned = BooleanArray(weights.size)
    fun share(i: Int): Float {
        val taken = weights.indices.filter { pinned[it] }.sumOf { minimums[it].toDouble() }
        val left = weights.indices.filter { !pinned[it] }.sumOf { weights[it].toDouble() }
        return ((free - taken) * weights[i] / left).toFloat()
    }
    var changed = true
    while (changed) {
        changed = false
        for (i in weights.indices) {
            if (!pinned[i] && share(i) < minimums[i]) {
                pinned[i] = true
                changed = true
            }
        }
    }
    return weights.indices.map { if (pinned[it]) minimums[it] else share(it) }.map { it.coerceAtLeast(1f) }
}
