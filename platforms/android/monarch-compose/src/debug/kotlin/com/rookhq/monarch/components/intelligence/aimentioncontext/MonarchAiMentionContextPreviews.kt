package com.rookhq.monarch.components.intelligence.aimentioncontext

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.collaboration.mentionpicker.MonarchMentionPickerStatus
import com.rookhq.monarch.components.collaboration.mentionpicker.MonarchMentionQuery
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiMentionContextPreview() {
    PreviewFrame {
        MonarchAiMentionContext(
            query = MonarchMentionQuery.find("Summarize @q3"),
            items = listOf(
                MonarchAiMentionItem("doc-1", MonarchAiMentionKind.Doc, "Q3 report.pdf", "Finance", tokens = 12_400),
                MonarchAiMentionItem("user-1", MonarchAiMentionKind.Person, "Ana Lopez", "Finance lead"),
                MonarchAiMentionItem("deal-1", MonarchAiMentionKind.Record, "Acme renewal", enabled = false),
            ),
            onSelect = {},
        )
        MonarchAiMentionContext(
            query = MonarchMentionQuery.find("@q3"),
            items = emptyList(),
            onSelect = {},
            status = MonarchMentionPickerStatus.Failed,
            onRetry = {},
        )
    }
}
