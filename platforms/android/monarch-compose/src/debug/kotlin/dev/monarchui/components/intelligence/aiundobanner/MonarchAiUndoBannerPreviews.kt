package dev.monarchui.components.intelligence.aiundobanner

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiUndoBannerPreview() {
    PreviewFrame {
        MonarchAiUndoBanner(
            title = "AI changed 2 fields",
            onUndo = {},
            description = "On the Acme Ltd deal",
            changes = listOf(MonarchAiChange("Status", "Open", "Closed"), MonarchAiChange("Owner", "Ana", "Sam")),
            onAudit = {},
            onDismiss = {},
        )
        MonarchAiUndoBanner(
            title = "AI changed 5 fields",
            onUndo = {},
            changes = List(5) { MonarchAiChange("Field ${it + 1}", "Old", "New") },
        )
    }
}
