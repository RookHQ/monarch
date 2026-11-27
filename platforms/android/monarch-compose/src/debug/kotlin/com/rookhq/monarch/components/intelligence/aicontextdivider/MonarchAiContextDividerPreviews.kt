package com.rookhq.monarch.components.intelligence.aicontextdivider

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
