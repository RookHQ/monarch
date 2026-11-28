package dev.monarchui.sample.sections.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.files.fileupload.MonarchPickedFile
import dev.monarchui.components.files.mediaupload.MonarchMediaUpload
import dev.monarchui.components.files.mediaupload.MonarchMediaUploadContext
import dev.monarchui.components.files.mediaupload.MonarchMediaUploadKind
import dev.monarchui.components.files.mediaupload.MonarchMediaUploadResult
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import kotlinx.coroutines.delay

/** The catalogue page of MediaUpload: the "upload" only waits and reports progress; nothing leaves the device. */
object MonarchMediaUploadSection : CataloguePage {
    override val title = "MediaUpload"

    private const val STEPS = 10
    private const val STEP_MILLIS = 150L
    private const val MAX_IMAGE = 5L * 1024 * 1024
    private const val MAX_VIDEO = 50L * 1024 * 1024
    private const val MAX_LENGTH = 90_000L

    @Composable
    override fun Content() {
        var cover by rememberSaveable { mutableStateOf<String?>(null) }
        var hero by rememberSaveable { mutableStateOf<String?>(null) }
        val upload: suspend (MonarchPickedFile, MonarchMediaUploadContext) -> MonarchMediaUploadResult =
            { file, context ->
                for (step in 1..STEPS) {
                    delay(STEP_MILLIS)
                    context.reportProgress(step.toFloat() / STEPS)
                }
                MonarchMediaUploadResult(file.uri.toString())
            }
        MonarchMediaUpload(
            label = "Cover image",
            value = cover,
            onValueChange = { cover = it },
            onUpload = upload,
            helperText = "Shown at the top of the article.",
            maxImageSize = MAX_IMAGE,
            videoUnavailable = "Videos are available on the Pro plan.",
        )
        CatalogueCaption("Image or video")
        MonarchMediaUpload(
            label = "Hero media",
            value = hero,
            onValueChange = { hero = it },
            onUpload = upload,
            kinds = setOf(MonarchMediaUploadKind.Image, MonarchMediaUploadKind.Video),
            maxImageSize = MAX_IMAGE,
            maxVideoSize = MAX_VIDEO,
            maxVideoDurationMillis = MAX_LENGTH,
        )
        CatalogueCaption("Disabled")
        MonarchMediaUpload(
            label = "Cover image",
            value = null,
            onValueChange = {},
            onUpload = upload,
            enabled = false,
        )
    }
}
