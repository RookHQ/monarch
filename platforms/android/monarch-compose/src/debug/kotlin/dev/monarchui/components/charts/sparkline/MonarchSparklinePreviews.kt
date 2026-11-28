package dev.monarchui.components.charts.sparkline

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val Trend = listOf(3.0, 5.0, 4.0, 8.0, 7.0, 9.0, 6.0, 11.0)

@MonarchPreviews
@Composable
internal fun MonarchSparklineVariantsPreview() {
    PreviewFrame {
        MonarchSparklineVariant.entries.forEach { variant ->
            MonarchSparkline(values = Trend, variant = variant, label = variant.name)
        }
        MonarchSparkline(values = listOf(4.0, -2.0, 6.0, 3.0, -1.0, 5.0), variant = MonarchSparklineVariant.Bar)
    }
}

@MonarchPreviews
@Composable
internal fun MonarchSparklineTonesPreview() {
    PreviewFrame {
        MonarchTone.entries.forEach { tone ->
            MonarchSparkline(values = listOf(120.0, 180.0, null, 260.0, 410.0), tone = tone, fullWidth = true)
        }
        MonarchSparkline(values = listOf(1.0))
    }
}
