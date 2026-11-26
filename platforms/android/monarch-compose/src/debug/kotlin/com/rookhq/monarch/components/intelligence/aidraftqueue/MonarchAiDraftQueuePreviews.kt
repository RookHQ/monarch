package com.rookhq.monarch.components.intelligence.aidraftqueue

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiDraftQueuePreview() {
    PreviewFrame {
        MonarchAiDraftQueue(
            drafts = listOf(
                MonarchAiDraft(
                    id = "1",
                    title = "Reply to Alex Morgan",
                    body = "Thanks for flagging this, Alex. The refund was issued today.",
                    context = "Alex: I was charged twice for my order.",
                ),
                MonarchAiDraft("2", "Follow-up to Priya", "", status = MonarchAiDraftStatus.Regenerating),
                MonarchAiDraft("3", "Note for Sam", "Moved the call.", status = MonarchAiDraftStatus.Approved),
                MonarchAiDraft("4", "Reply to Lee", "Not needed.", status = MonarchAiDraftStatus.Skipped),
            ),
            onApprove = {},
            onEdit = { _, _ -> },
            onRegenerate = {},
            onSkip = {},
            title = "Replies to review",
            onSend = {},
        )
    }
}
