package com.rookhq.monarch.components.charts.heatmap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.R
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrame
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.components.charts.chartframe.MonarchChartTable
import com.rookhq.monarch.components.charts.chartframe.MonarchChartTableSeries
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchChartValueFormat
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Locale

/**
 * A matrix of values as shaded cells, such as activity by weekday and hour,
 * with a Less to More legend.
 *
 * Built by Monarch inside `MonarchChartFrame`: Material 3 has no chart. Values
 * fall into five levels: zero on the muted background and four mixes of one
 * data colour; a position without a value is an outlined cell. The cells are
 * drawn on a `Canvas`, mirrored right to left. The matrix is a traversal group
 * named with a generated summary (its size and highest cell), and every cell
 * is a TalkBack element read as its row, column and value, row by row. A tap
 * selects a cell, outlines it and writes its row, column and value under the
 * matrix, announced politely. The legend reads the range of every step, and
 * "View as table" lists every value.
 *
 * @param title the chart's name, shown as the frame's heading.
 * @param rows the rows, top to bottom.
 * @param columns the columns, start to end.
 * @param cells the values in any order. A row and column pair without a cell is no data; cells whose keys are not
 * in [rows] or [columns] are ignored, and the later of two cells at one position wins.
 * @param modifier the modifier of the frame.
 * @param description one line under the title, such as the period.
 * @param status what the frame shows. A ready chart without any value shows the empty state.
 * @param color the data colour of the levels.
 * @param thresholds three ascending values where levels 2, 3 and 4 start. `null` uses quarters of the largest
 * value.
 * @param showValues writes each value in its cell when it fits at the current text size.
 * @param showLegend shows the Less to More legend under the matrix.
 * @param summary replaces the generated summary that TalkBack reads for the matrix.
 * @param emptyMessage the message of the empty state.
 * @param footer notes under the chart, such as the source.
 * @param formatValue writes a value. `null` writes a number in the current locale.
 * @param onRetry shows Try again in the error status.
 * @param onSelect called with the tapped cell, or the cell chosen with the TalkBack Select action; its value is
 * `null` when it has no data.
 * @param labels replacements for the built-in strings, including the frame's.
 */
@Composable
@Suppress("LongParameterList", "LongMethod")
public fun MonarchHeatmap(
    title: String,
    rows: List<MonarchHeatmapAxisItem>,
    columns: List<MonarchHeatmapAxisItem>,
    cells: List<MonarchHeatmapCell>,
    modifier: Modifier = Modifier,
    description: String? = null,
    status: MonarchChartFrameStatus = MonarchChartFrameStatus.Ready,
    color: MonarchChartColor = MonarchChartColor.Data1,
    thresholds: List<Double>? = null,
    showValues: Boolean = false,
    showLegend: Boolean = true,
    summary: String? = null,
    emptyMessage: String? = null,
    footer: String? = null,
    formatValue: ((Double) -> String)? = null,
    onRetry: (() -> Unit)? = null,
    onSelect: ((MonarchHeatmapCell) -> Unit)? = null,
    labels: MonarchHeatmapLabels = MonarchHeatmapLabels(),
) {
    val locale = LocalConfiguration.current.locales[0]
    val write = rememberMonarchChartValueFormat(formatValue)
    val words = heatmapWords(labels, locale, write)
    val model = remember(rows, columns, cells, thresholds) { HeatmapModel(rows, columns, cells, thresholds) }
    val table = MonarchChartTable(
        categoryTitle = monarchString(labels.rowHeader, R.string.monarch_heatmap_row_header),
        categories = rows.map { it.label },
        series = columns.indices.map { c ->
            MonarchChartTableSeries(
                name = columns[c].label,
                values = rows.indices.map { r -> model.valueAt(r, c) },
                formattedValues = rows.indices.map { r -> HeatmapText.valueText(model.cellAt(r, c), words) },
            )
        },
    )
    var selectedRow by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedColumn by rememberSaveable { mutableStateOf<String?>(null) }
    val rowIndex = rows.indexOfFirst { it.key == selectedRow }.takeIf { it >= 0 }
    val columnIndex = columns.indexOfFirst { it.key == selectedColumn }.takeIf { it >= 0 }
    val choose: (Int, Int) -> Unit = { r, c ->
        selectedRow = rows[r].key
        selectedColumn = columns[c].key
        onSelect?.invoke(model.cellAt(r, c))
    }
    val density = LocalDensity.current
    var measured by remember { mutableStateOf<Dp?>(null) }
    MonarchChartFrame(
        title = title,
        modifier = modifier,
        description = description,
        status = if (status == MonarchChartFrameStatus.Ready && !model.hasData) {
            MonarchChartFrameStatus.Empty
        } else {
            status
        },
        table = table,
        emptyMessage = emptyMessage,
        footer = footer,
        height = maxOf(MonarchHeatmapTokens.MinHeight, measured ?: MonarchHeatmapTokens.MinHeight),
        onRetry = onRetry,
        labels = labels.frame,
    ) {
        val spoken = summary ?: HeatmapText.summary(model, words)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .onSizeChanged { measured = with(density) { it.height.toDp() } }
                .semantics {
                    isTraversalGroup = true
                    contentDescription = spoken
                    collectionInfo = CollectionInfo(rowCount = rows.size, columnCount = columns.size)
                },
            verticalArrangement = Arrangement.spacedBy(MonarchHeatmapTokens.SectionGap),
        ) {
            HeatmapMatrix(
                model = model,
                words = words,
                style = HeatmapMatrixStyle(color, showValues, rowIndex, columnIndex),
                onTap = choose,
                selectLabel = monarchString(labels.select, R.string.monarch_heatmap_select),
                onSelect = if (onSelect != null) choose else null,
            )
            val detail = if (rowIndex != null && columnIndex != null) {
                HeatmapText.describe(model, rowIndex, columnIndex, words)
            } else {
                null
            }
            // Always present, so TalkBack announces the details of each newly selected cell.
            Box(
                Modifier.clearAndSetSemantics {
                    liveRegion = LiveRegionMode.Polite
                    if (detail != null) contentDescription = detail
                },
            ) {
                if (detail != null) {
                    Text(
                        text = detail,
                        style = MonarchTheme.typography.caption,
                        color = MonarchHeatmapTokens.valueColor(MonarchTheme.colors),
                    )
                }
            }
            if (showLegend) {
                HeatmapLegend(
                    slot = color,
                    showEmpty = model.hasGaps,
                    words = HeatmapLegendWords(
                        name = monarchString(labels.frame.legend, R.string.monarch_chartlegend_legend),
                        less = monarchString(labels.less, R.string.monarch_heatmap_less),
                        more = monarchString(labels.more, R.string.monarch_heatmap_more),
                        steps = (if (model.hasGaps) listOf(words.noData) else emptyList()) +
                            HeatmapText.legendSteps(model.scale, words),
                    ),
                )
            }
        }
    }
}

/** The built-in strings, or their replacements. */
@Composable
private fun heatmapWords(labels: MonarchHeatmapLabels, locale: Locale, format: (Double) -> String): HeatmapWords {
    val resources = LocalResources.current
    return HeatmapWords(
        locale = locale,
        format = format,
        noData = monarchString(labels.noData, R.string.monarch_heatmap_no_data),
        rangeBelow = monarchString(labels.rangeBelow, R.string.monarch_heatmap_range_below),
        above = monarchString(labels.above, R.string.monarch_heatmap_above),
        range = monarchString(labels.range, R.string.monarch_heatmap_range),
        rangeAbove = monarchString(labels.rangeAbove, R.string.monarch_heatmap_range_above),
        rowCount = labels.rowCount ?: { resources.getQuantityString(R.plurals.monarch_heatmap_row_count, it, it) },
        columnCount = labels.columnCount
            ?: { resources.getQuantityString(R.plurals.monarch_heatmap_column_count, it, it) },
        size = monarchString(labels.size, R.string.monarch_heatmap_size),
        highest = monarchString(labels.highest, R.string.monarch_heatmap_highest),
        cell = monarchString(labels.cell, R.string.monarch_heatmap_cell),
        withNote = monarchString(labels.withNote, R.string.monarch_heatmap_with_note),
    )
}
