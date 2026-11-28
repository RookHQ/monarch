package dev.monarchui.components.files.videothumbnail

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.ColorPainter
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchVideoThumbnailPreview() {
    PreviewFrame {
        MonarchVideoThumbnail(
            title = "Product tour",
            image = ColorPainter(MonarchTheme.colors.data1),
            onPlay = {},
            durationMillis = 65_000L,
        )
        MonarchVideoThumbnail(title = "Loading", image = null, durationMillis = 3_725_000L)
    }
}
