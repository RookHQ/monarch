package com.rookhq.monarch.components.files.attachmentchip

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchAttachmentChipPreview() {
    val close: @Composable () -> Unit = {
        MonarchIcon(PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm)
    }
    PreviewFrame {
        MonarchAttachmentChip(name = "Quarterly report.pdf", sizeBytes = 482_000L, onOpen = {}, onRemove = {})
        MonarchAttachmentChip(
            name = "photo.jpg",
            status = MonarchUploadStatus.Uploading,
            progress = 0.45f,
            onRemove = {},
        )
        MonarchAttachmentChip(
            name = "notes.txt",
            status = MonarchUploadStatus.Queued,
            onRemove = {},
            removeIcon = close,
        )
        MonarchAttachmentChip(
            name = "budget.xlsx",
            status = MonarchUploadStatus.Failed,
            errorMessage = "File is too large",
            onRetry = {},
            onRemove = {},
        )
        MonarchAttachmentChip(name = "draft.docx", status = MonarchUploadStatus.Cancelled)
    }
}
