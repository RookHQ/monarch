package com.rookhq.monarch.components.datadisplay.widget

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.datadisplay.metric.MonarchMetric
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
