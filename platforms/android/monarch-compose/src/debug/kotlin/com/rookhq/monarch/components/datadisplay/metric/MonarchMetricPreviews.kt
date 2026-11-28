package com.rookhq.monarch.components.datadisplay.metric

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.rookhq.monarch.components.charts.deltaindicator.MonarchDeltaIndicatorGoodDirection
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchMetricPreview() {
    PreviewFrame {
        MonarchMetric(
            label = "Latency",
            value = 840.0,
            unit = "ms",
            change = MonarchMetricChange(-0.08, "vs last week", MonarchDeltaIndicatorGoodDirection.Down),
        )
        MonarchMetric(
            label = "Revenue",
            value = 3_400_000.0,
            format = MonarchMetricFormat.Currency("EUR"),
            size = MonarchMetricSize.Lg,
            compact = true,
        )
        MonarchMetric(
            label = "Conversion",
            value = 0.12,
            format = MonarchMetricFormat.Percent,
            layout = MonarchMetricLayout.Inline,
        )
        MonarchMetric(label = "Errors", value = null, size = MonarchMetricSize.Sm, alignment = Alignment.End)
    }
}
