package dev.monarchui.sample.sections.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import dev.monarchui.components.files.attachmentchip.MonarchUploadStatus
import dev.monarchui.components.files.fileupload.MonarchFileUpload
import dev.monarchui.components.files.fileupload.MonarchFileUploadPicker
import dev.monarchui.components.files.fileupload.MonarchFileUploadVariant
import dev.monarchui.components.files.uploadqueue.MonarchUploadQueue
import dev.monarchui.components.files.uploadqueue.MonarchUploadQueueItem
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of FileUpload: the chosen files go into an upload queue (nothing is uploaded). */
object MonarchFileUploadSection : CataloguePage {
    override val title = "FileUpload"

    private const val MAX_FILES = 3
    private const val MAX_SIZE = 10L * 1024 * 1024

    @Composable
    override fun Content() {
        val queue = remember { mutableStateListOf<MonarchUploadQueueItem>() }
        MonarchFileUpload(
            onFiles = { accepted, _ ->
                accepted.forEach {
                    queue += MonarchUploadQueueItem(
                        id = it.uri.toString(),
                        name = it.name,
                        status = MonarchUploadStatus.Done,
                        size = it.size,
                        contentType = it.mimeType,
                    )
                }
            },
            accept = listOf("application/pdf", "image/*"),
            maxSize = MAX_SIZE,
            maxFiles = MAX_FILES,
            currentCount = queue.size,
        )
        if (queue.isNotEmpty()) {
            MonarchUploadQueue(items = queue.toList(), onRemove = { id -> queue.removeAll { it.id == id } })
        }
        CatalogueCaption("Photo picker, compact")
        MonarchFileUpload(
            onFiles = { _, _ -> },
            variant = MonarchFileUploadVariant.Compact,
            picker = MonarchFileUploadPicker.Media,
            accept = listOf("image/*"),
        )
        CatalogueCaption("Disabled")
        MonarchFileUpload(onFiles = { _, _ -> }, enabled = false)
    }
}
