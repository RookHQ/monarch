package com.rookhq.monarch.components.intelligence.aiattachmenttray

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiAttachmentTrayPreview() {
    PreviewFrame {
        MonarchAiAttachmentTray(
            files = listOf(
                MonarchAiAttachmentTrayFile("1", "report.pdf", sizeBytes = 482_000, contentType = "application/pdf"),
                MonarchAiAttachmentTrayFile("2", "photo.png", progress = 0.4f),
                MonarchAiAttachmentTrayFile("3", "data.csv", errorMessage = "File is too large"),
            ),
            onRemove = {},
            onRetry = {},
        )
    }
}
