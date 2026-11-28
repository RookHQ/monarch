package dev.monarchui.components.intelligence.aicontextdivider

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiContextDividerPreview() {
    PreviewFrame {
        MonarchAiContextDivider(kind = MonarchAiContextDividerKind.Day, dateMillis = System.currentTimeMillis())
        MonarchAiContextDivider(kind = MonarchAiContextDividerKind.ContextReset)
        MonarchAiContextDivider(
            kind = MonarchAiContextDividerKind.Summarized,
            summary = "The team agreed to ship the report on Friday.",
            onExpand = {},
        )
    }
}
