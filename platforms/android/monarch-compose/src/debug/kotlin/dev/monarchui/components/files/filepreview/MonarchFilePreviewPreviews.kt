package dev.monarchui.components.files.filepreview

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.ColorPainter
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchFilePreviewPreview() {
    PreviewFrame {
        MonarchFilePreview(name = "notes.md", text = "# Notes\nShip the beta on Friday.")
        MonarchFilePreview(name = "diagram.png", image = ColorPainter(MonarchTheme.colors.data2))
        MonarchFilePreview(name = "archive.zip", onDownload = {})
    }
}
