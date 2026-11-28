package dev.monarchui.sample.sections.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.files.attachmentchip.MonarchAttachmentChip
import dev.monarchui.components.files.attachmentchip.MonarchUploadStatus
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import kotlinx.coroutines.delay

/** The catalogue page of AttachmentChip: an upload that runs, fails once and can be retried. */
object MonarchAttachmentChipSection : CataloguePage {
    override val title = "AttachmentChip"

    private const val STEP = 0.1f
    private const val STEP_MILLIS = 250L
    private const val FAIL_AT = 0.6f
    private const val REPORT_BYTES = 482_000L

    @Composable
    override fun Content() {
        var progress by rememberSaveable { mutableFloatStateOf(0f) }
        var status by rememberSaveable { mutableStateOf(MonarchUploadStatus.Uploading) }
        var failedOnce by rememberSaveable { mutableStateOf(false) }
        var removed by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(status) {
            while (status == MonarchUploadStatus.Uploading && progress < 1f) {
                delay(STEP_MILLIS)
                progress = minOf(1f, progress + STEP)
                if (progress >= FAIL_AT && !failedOnce) {
                    failedOnce = true
                    status = MonarchUploadStatus.Failed
                }
            }
            if (status == MonarchUploadStatus.Uploading) status = MonarchUploadStatus.Done
        }
        CatalogueCaption("An upload that fails once")
        if (!removed) {
            MonarchAttachmentChip(
                name = "Quarterly report.pdf",
                status = status,
                sizeBytes = REPORT_BYTES,
                progress = progress,
                onRetry = { status = MonarchUploadStatus.Uploading },
                onRemove = { removed = true },
            )
        }
        CatalogueCaption("Queued and cancelled")
        MonarchAttachmentChip(name = "notes.txt", status = MonarchUploadStatus.Queued)
        MonarchAttachmentChip(name = "draft.docx", status = MonarchUploadStatus.Cancelled)
    }
}
