package com.rookhq.monarch.components.files.editortoolbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
