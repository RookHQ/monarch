package dev.monarchui.components.datadisplay.widget

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.monarchui.components.datadisplay.metric.MonarchMetric
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchWidgetPreview() {
    PreviewFrame {
        MonarchWidget(
            title = "Revenue",
            period = "Last 30 days",
            footer = "Gross, before refunds",
            onViewAll = {},
            menu = { close -> DropdownMenuItem(text = { Text("Export") }, onClick = close) },
        ) {
            MonarchMetric(label = "Total", value = 128_400.0)
        }
        MonarchWidget(title = "Signups", state = MonarchWidgetState.Empty()) {}
        MonarchWidget(title = "Churn", state = MonarchWidgetState.Failed(), onRetry = {}) {}
        MonarchWidget(title = "Churn", variant = MonarchWidgetVariant.Plain, state = MonarchWidgetState.Failed()) {}
    }
}
