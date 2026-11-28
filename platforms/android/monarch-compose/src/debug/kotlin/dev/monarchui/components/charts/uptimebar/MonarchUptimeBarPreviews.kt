package dev.monarchui.components.charts.uptimebar

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val days = (1..30).map { day ->
    MonarchUptimeBarSegment(
        id = "2026-03-$day",
        label = "Mar $day",
        uptime = when (day) {
            12 -> 99.42
            24 -> 96.4
            else -> 100.0
        },
        note = if (day == 24) "Database failover" else null,
    )
}

@MonarchPreviews
@Composable
internal fun MonarchUptimeBarPreview() {
    PreviewFrame {
        MonarchUptimeBar(
            segments = days,
            label = "Search API",
            description = "Last 30 days",
            startLabel = "30 days ago",
            endLabel = "Today",
            showLegend = true,
        )
        MonarchUptimeBar(
            segments = days.take(14),
            label = "Website",
            size = MonarchUptimeBarSize.Sm,
            activeIndex = 11,
            onActiveIndexChange = {},
        )
    }
}
