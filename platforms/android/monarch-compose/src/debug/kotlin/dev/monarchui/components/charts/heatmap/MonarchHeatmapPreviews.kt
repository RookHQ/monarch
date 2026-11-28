package dev.monarchui.components.charts.heatmap

import androidx.compose.runtime.Composable
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val previewRows = listOf("Mon", "Tue", "Wed").map { MonarchHeatmapAxisItem(it) }
private val previewColumns = listOf("9:00", "10:00", "11:00", "12:00").map { MonarchHeatmapAxisItem(it) }
private val previewCells = previewRows.flatMapIndexed { r, row ->
    previewColumns.mapIndexed { c, column ->
        MonarchHeatmapCell(row.key, column.key, if (r == 2 && c == 3) null else ((r + 1) * (c + 2)).toDouble())
    }
}

@MonarchPreviews
@Composable
internal fun MonarchHeatmapPreview() {
    PreviewFrame {
        MonarchHeatmap(
            title = "Support tickets",
            rows = previewRows,
            columns = previewColumns,
            cells = previewCells,
            description = "This week",
        )
        MonarchHeatmap(
            title = "Hours logged",
            rows = previewRows,
            columns = previewColumns,
            cells = previewCells,
            color = MonarchChartColor.Data2,
            thresholds = listOf(4.0, 8.0, 12.0),
            showValues = true,
            onSelect = {},
        )
    }
}
