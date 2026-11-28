package com.rookhq.monarch.sample.sections.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.files.attachmentchip.MonarchUploadStatus
import com.rookhq.monarch.components.files.uploadqueue.MonarchUploadQueue
import com.rookhq.monarch.components.files.uploadqueue.MonarchUploadQueueItem
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import kotlinx.coroutines.delay

/** The catalogue page of UploadQueue: a simulated transfer, since the catalogue uploads nothing. */
object MonarchUploadQueueSection : CataloguePage {
    override val title = "UploadQueue"

    private const val TICK_MILLIS = 400L
    private const val STEP = 0.1f
    private const val REPORT_SIZE = 2_400_000L
    private const val PHOTO_SIZE = 800_000L
    private const val VIDEO_SIZE = 120_000_000L

    private fun startItems() = listOf(
        MonarchUploadQueueItem(id = "1", name = "Q3 report.pdf", status = MonarchUploadStatus.Done, size = REPORT_SIZE),
        MonarchUploadQueueItem(
            id = "2",
            name = "photo.jpg",
            status = MonarchUploadStatus.Uploading,
            size = PHOTO_SIZE,
            progress = 0f,
        ),
        MonarchUploadQueueItem(
            id = "3",
            name = "launch.mov",
            status = MonarchUploadStatus.Failed,
            size = VIDEO_SIZE,
            errorMessage = "Too large",
        ),
    )

    @Composable
    override fun Content() {
        val items = remember { mutableStateListOf<MonarchUploadQueueItem>().apply { addAll(startItems()) } }
        val uploading = items.any { it.status == MonarchUploadStatus.Uploading }
        LaunchedEffect(uploading) {
            // Runs only while a file uploads, so the page is idle otherwise.
            while (items.any { it.status == MonarchUploadStatus.Uploading }) {
                delay(TICK_MILLIS)
                val index = items.indexOfFirst { it.status == MonarchUploadStatus.Uploading }
                if (index >= 0) {
                    val item = items[index]
                    val next = (item.progress ?: 0f) + STEP
                    items[index] = if (next >= 1f) {
                        item.copy(status = MonarchUploadStatus.Done, progress = null)
                    } else {
                        item.copy(progress = next)
                    }
                }
            }
        }
        CatalogueCaption("A simulated transfer; retry restarts a file")
        MonarchUploadQueue(
            items = items.toList(),
            onCancel = { id -> update(items, id) { it.copy(status = MonarchUploadStatus.Cancelled, progress = null) } },
            onRetry = { id -> update(items, id) { it.copy(status = MonarchUploadStatus.Uploading, progress = 0f) } },
            onRemove = { id -> items.removeAll { it.id == id } },
            onClearFinished = { items.removeAll { !it.status.active } },
        )
        MonarchButton(text = "Start again", onClick = {
            items.clear()
            items.addAll(startItems())
        })
    }

    private fun update(
        items: MutableList<MonarchUploadQueueItem>,
        id: String,
        change: (MonarchUploadQueueItem) -> MonarchUploadQueueItem,
    ) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) items[index] = change(items[index])
    }
}
