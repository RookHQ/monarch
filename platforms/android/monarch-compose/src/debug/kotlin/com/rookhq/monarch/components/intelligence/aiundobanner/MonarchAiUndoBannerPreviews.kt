package com.rookhq.monarch.components.intelligence.aiundobanner

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
