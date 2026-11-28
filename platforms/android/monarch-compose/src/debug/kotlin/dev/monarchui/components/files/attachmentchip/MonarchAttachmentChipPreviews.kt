package dev.monarchui.components.files.attachmentchip

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

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
