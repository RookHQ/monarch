package dev.monarchui.components.files.mediaupload

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchMediaUploadPreview() {
    PreviewFrame {
        MonarchMediaUpload(
            label = "Cover image",
            value = null,
            onValueChange = {},
            onUpload = { _, _ -> MonarchMediaUploadResult("https://example.com/cover.jpg") },
            maxImageSize = 5_000_000L,
        )
        MonarchMediaUpload(
            label = "Cover image",
            value = "https://example.com/cover.jpg",
            onValueChange = {},
            onUpload = { _, _ -> MonarchMediaUploadResult("https://example.com/cover.jpg") },
            helperText = "Shown at the top of the page.",
        )
        MonarchMediaUpload(
            label = "Intro video",
            value = "https://example.com/intro.mp4",
            onValueChange = {},
            onUpload = { _, _ -> MonarchMediaUploadResult("https://example.com/intro.mp4") },
            kinds = setOf(MonarchMediaUploadKind.Image, MonarchMediaUploadKind.Video),
            onPlay = {},
        )
    }
}
