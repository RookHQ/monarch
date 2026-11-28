package dev.monarchui.components.datadisplay.statcard

import androidx.compose.runtime.Composable
import dev.monarchui.components.charts.sparkline.MonarchSparkline
import dev.monarchui.components.datadisplay.metric.MonarchMetricChange
import dev.monarchui.components.datadisplay.metric.MonarchMetricFormat
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import java.util.Locale

@MonarchPreviews
@Composable
internal fun MonarchStatCardPreview() {
    PreviewFrame {
        MonarchStatCard(
            label = "Revenue",
            value = 48_200.0,
            format = MonarchMetricFormat.Currency("EUR"),
            locale = Locale.UK,
            change = MonarchMetricChange(0.12),
            caption = "vs last month",
            help = "Invoiced revenue, before refunds.",
            sparkline = { MonarchSparkline(values = listOf(3.0, 5.0, 4.0, 7.0, 8.0)) },
            onClick = {},
        )
        MonarchStatCard(label = "Churn", value = 0.031, format = MonarchMetricFormat.Percent, tone = MonarchTone.Danger)
        MonarchStatCard(label = "Open tickets", value = null, size = MonarchStatCardSize.Sm, highlighted = true)
        MonarchStatCard(label = "Uptime", value = null, loading = true)
        MonarchStatCard(label = "Signups", value = null, error = "The server did not answer.", onRetry = {})
    }
}
