package com.rookhq.monarch.components.files.filepreview

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.ColorPainter
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchFilePreviewPreview() {
    PreviewFrame {
        MonarchFilePreview(name = "notes.md", text = "# Notes\nShip the beta on Friday.")
        MonarchFilePreview(name = "diagram.png", image = ColorPainter(MonarchTheme.colors.data2))
        MonarchFilePreview(name = "archive.zip", onDownload = {})
    }
}
