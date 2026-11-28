package dev.monarchui.components.charts.gauge

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val Health = listOf(
    MonarchGaugeBand(0.0..70.0, MonarchTone.Success, "Healthy"),
    MonarchGaugeBand(70.0..90.0, MonarchTone.Warning, "At risk"),
    MonarchGaugeBand(90.0..100.0, MonarchTone.Danger, "Critical"),
)

@MonarchPreviews
@Composable
internal fun MonarchGaugeArcPreview() {
    PreviewFrame {
        MonarchGauge(value = 72.0, bands = Health, target = 80.0, label = "CPU", description = "Last hour")
        MonarchGauge(value = 0.55, range = 0.0..1.0, size = MonarchGaugeSize.Sm, label = "Quota used")
    }
}

@MonarchPreviews
@Composable
internal fun MonarchGaugeNeedlePreview() {
    PreviewFrame {
        MonarchGauge(value = 40.0, bands = Health, indicator = MonarchGaugeIndicator.Needle, label = "Score")
        MonarchGauge(value = null, size = MonarchGaugeSize.Lg, label = "Memory")
    }
}
