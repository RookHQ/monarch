package dev.monarchui.components.billing.usagemeter

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchUsageMeterPreview() {
    PreviewFrame {
        MonarchUsageMeter(
            items = listOf(
                MonarchUsageMeterItem("Requests", used = 4200.0, limit = 10000.0),
                MonarchUsageMeterItem("Storage", used = 42.0, limit = 50.0, unit = "GB"),
                MonarchUsageMeterItem("Minutes", used = 1300.0, limit = 1000.0, description = "Calls and recordings"),
                MonarchUsageMeterItem("Seats", used = 4.0),
            ),
            title = "Usage",
            period = "This month",
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchUsageMeterStatesPreview() {
    PreviewFrame {
        MonarchUsageMeter(items = emptyList(), title = "Usage", loading = true)
        MonarchUsageMeter(items = emptyList(), error = "Usage could not be loaded.", onRetry = {})
    }
}
