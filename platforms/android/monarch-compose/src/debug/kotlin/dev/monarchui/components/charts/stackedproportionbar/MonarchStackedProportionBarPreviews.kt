package dev.monarchui.components.charts.stackedproportionbar

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchStackedProportionBarPreview() {
    PreviewFrame {
        MonarchStackedProportionBar(
            label = "Lead pipeline",
            segments = listOf(
                MonarchStackedProportionBarSegment("new", "New", 1240.0),
                MonarchStackedProportionBarSegment("campaign", "In a campaign", 610.0),
                MonarchStackedProportionBarSegment("replied", "Replied", 182.0),
                MonarchStackedProportionBarSegment("converted", "Converted", 41.0),
            ),
        )
        MonarchStackedProportionBar(
            label = "Storage",
            segments = listOf(
                MonarchStackedProportionBarSegment("media", "Media", 42.0),
                MonarchStackedProportionBarSegment("docs", "Documents", 18.0),
            ),
            total = 100.0,
            remainderLabel = "Free",
            legendContent = MonarchStackedProportionBarLegendContent.Value,
            size = MonarchStackedProportionBarSize.Lg,
            formatValue = { "${it.toInt()} GB" },
        )
        MonarchStackedProportionBar(
            label = "Monitors",
            segments = listOf(
                MonarchStackedProportionBarSegment("up", "Operational", 41.0, MonarchTone.Success),
                MonarchStackedProportionBarSegment("slow", "Degraded", 3.0, MonarchTone.Warning),
                MonarchStackedProportionBarSegment("down", "Down", 1.0, MonarchTone.Danger),
            ),
            size = MonarchStackedProportionBarSize.Sm,
        )
        MonarchStackedProportionBar(label = "Empty", segments = emptyList())
    }
}
