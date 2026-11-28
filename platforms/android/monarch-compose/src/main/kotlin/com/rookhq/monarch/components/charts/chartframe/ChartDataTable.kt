package com.rookhq.monarch.components.charts.chartframe

import android.icu.text.NumberFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.rookhq.monarch.R
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The data table of a chart: a header row and one row per category, with
 * equal columns so they line up. Each row is one TalkBack element that reads
 * the category and then every series with its value.
 */
@Composable
internal fun ChartDataTable(table: MonarchChartTable, noValue: String, modifier: Modifier = Modifier) {
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    val number = remember { NumberFormat.getInstance() }
    val format: (Double) -> String = { number.format(it) }
    Column(modifier = modifier.fillMaxWidth()) {
        val header = listOf(table.categoryTitle) + table.series.map { it.name }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {
                    contentDescription = header.joinToString(", ")
                    heading()
                },
        ) {
            header.forEachIndexed { index, text ->
                Cell(text, typography.label, colors.textDefault, numeric = index > 0)
            }
        }
        MonarchDivider()
        table.categories.forEachIndexed { row, category ->
            val spoken = listOf(category) + table.series.map { series ->
                stringResource(
                    R.string.monarch_chartframe_cell,
                    series.name,
                    MonarchChartFrameTokens.spokenCell(series, row, noValue, format),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = spoken.joinToString(", ") },
            ) {
                Cell(category, typography.caption, colors.textMuted, numeric = false)
                table.series.forEach { series ->
                    Cell(
                        MonarchChartFrameTokens.cellText(series, row, format),
                        typography.caption,
                        colors.textDefault,
                        numeric = true,
                    )
                }
            }
            MonarchDivider(variant = MonarchDividerVariant.Subtle)
        }
    }
}

/** One cell. Numbers align to the end, as in every table. */
@Composable
private fun RowScope.Cell(text: String, style: TextStyle, color: Color, numeric: Boolean) {
    Text(
        text = text,
        modifier = Modifier
            .weight(1f)
            .padding(vertical = MonarchChartFrameTokens.CellPadding),
        style = style,
        color = color,
        textAlign = if (numeric) TextAlign.End else TextAlign.Start,
    )
}
