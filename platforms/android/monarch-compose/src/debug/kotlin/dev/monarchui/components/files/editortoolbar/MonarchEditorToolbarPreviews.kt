package dev.monarchui.components.files.editortoolbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchEditorToolbarPreview() {
    val icon = rememberVectorPainter(PreviewIcon)
    PreviewFrame {
        MonarchEditorToolbar(
            onAction = {},
            active = setOf(MonarchEditorTool.heading(2), MonarchEditorTool.Bold),
            disabled = setOf(MonarchEditorTool.Redo),
        )
        MonarchEditorToolbar(
            onAction = {},
            groups = MonarchEditorTool.InlineGroups,
            active = setOf(MonarchEditorTool.Italic),
            icons = MonarchEditorTool.InlineGroups.flatten().associateWith { icon },
        )
        MonarchEditorToolbar(onAction = {}, groups = MonarchEditorTool.InlineGroups, enabled = false)
    }
}
