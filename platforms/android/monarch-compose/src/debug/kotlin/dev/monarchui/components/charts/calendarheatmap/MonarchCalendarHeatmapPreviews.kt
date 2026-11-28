package dev.monarchui.components.charts.calendarheatmap

import androidx.compose.runtime.Composable
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchCalendarHeatmapPreview() {
    val day = 86_400_000L
    val start = 20_454L * day
    val days = (0 until 90).map { index ->
        val weekend = (index + 4) % 7 == 0 || (index + 4) % 7 == 6
        MonarchCalendarHeatmapDay(
            date = start + index * day,
            value = if (weekend) null else (index * 7 % 13).toDouble(),
            note = if (index == 18) "Holiday" else null,
            inactive = weekend,
            marker = index == 18,
        )
    }
    PreviewFrame {
        MonarchCalendarHeatmap(
            title = "Contributions",
            days = days,
            description = "January to March 2026",
            today = start + 89 * day,
        )
        MonarchCalendarHeatmap(
            title = "Hours in January",
            days = days,
            from = start,
            to = start + 30 * day,
            layout = MonarchCalendarHeatmapLayout.Month,
            color = MonarchChartColor.Data3,
            today = start + 14 * day,
        )
    }
}
