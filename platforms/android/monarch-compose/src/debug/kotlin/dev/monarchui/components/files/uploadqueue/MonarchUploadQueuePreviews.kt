package dev.monarchui.components.files.uploadqueue

import androidx.compose.runtime.Composable
import dev.monarchui.components.files.attachmentchip.MonarchUploadStatus
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchUploadQueuePreview() {
    PreviewFrame {
        MonarchUploadQueue(
            items = listOf(
                MonarchUploadQueueItem(
                    id = "1",
                    name = "Q3 report.pdf",
                    status = MonarchUploadStatus.Done,
                    size = 2_400_000,
                ),
                MonarchUploadQueueItem(
                    id = "2",
                    name = "photo.jpg",
                    status = MonarchUploadStatus.Uploading,
                    size = 800_000,
                    progress = 0.4f,
                ),
                MonarchUploadQueueItem(
                    id = "3",
                    name = "big.mov",
                    status = MonarchUploadStatus.Failed,
                    errorMessage = "Too large",
                ),
            ),
            onCancel = {},
            onRetry = {},
            onRemove = {},
            onClearFinished = {},
        )
    }
}
