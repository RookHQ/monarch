package com.rookhq.monarch.components.intelligence.aigeneratedbadge

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiGeneratedBadgePreview() {
    PreviewFrame {
        MonarchAiGeneratedBadgeSize.entries.forEach { size ->
            MonarchAiGeneratedBadgeKind.entries.forEach { kind -> MonarchAiGeneratedBadge(kind = kind, size = size) }
        }
        MonarchAiGeneratedBadge(model = "gemini-2.5-pro", dateMillis = 1_741_608_000_000L)
    }
}
