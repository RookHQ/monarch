package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.intelligence.aiattachmenttray.MonarchAiAttachmentTray
import dev.monarchui.components.intelligence.aiattachmenttray.MonarchAiAttachmentTrayFile
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import kotlinx.coroutines.delay

private const val StepMillis = 400L
private const val Step = 0.2f

private fun startingFiles() = listOf(
    MonarchAiAttachmentTrayFile("1", "quarterly-report.pdf", sizeBytes = 482_000, contentType = "application/pdf"),
    MonarchAiAttachmentTrayFile("2", "photo.png", sizeBytes = 120_000, contentType = "image/png", progress = 0f),
    MonarchAiAttachmentTrayFile("3", "data.csv", contentType = "text/csv", errorMessage = "File is too large"),
)

/** The catalogue page of AiAttachmentTray, with a simulated upload. */
object MonarchAiAttachmentTraySection : CataloguePage {
    override val title = "AiAttachmentTray"

    @Composable
    override fun Content() {
        var files by remember { mutableStateOf(startingFiles()) }
        LaunchedEffect(files) {
            val uploading = files.firstOrNull { (it.progress ?: 1f) < 1f } ?: return@LaunchedEffect
            delay(StepMillis)
            val next = (uploading.progress ?: 0f) + Step
            files = files.map { if (it.id == uploading.id) it.copy(progress = if (next >= 1f) null else next) else it }
        }
        CatalogueCaption("Retry restarts the failed upload")
        MonarchAiAttachmentTray(
            files = files,
            onRemove = { id -> files = files.filterNot { it.id == id } },
            onRetry = { id ->
                files = files.map { if (it.id == id) it.copy(errorMessage = null, progress = 0f) else it }
            },
        )
        MonarchButton(text = "Reset", onClick = { files = startingFiles() })
    }
}
