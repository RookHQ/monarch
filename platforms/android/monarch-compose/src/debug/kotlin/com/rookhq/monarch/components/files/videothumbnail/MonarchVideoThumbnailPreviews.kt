package com.rookhq.monarch.components.files.videothumbnail

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.ColorPainter
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

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
