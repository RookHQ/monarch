package com.rookhq.monarch.components.interaction.draghandle

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchDragHandlePreview() {
    PreviewFrame {
        Row {
            MonarchDragHandle(label = "Reorder Design review", onMove = {})
            MonarchDragHandle(label = "Reorder", state = MonarchDragHandleState.Grabbed, onMove = {})
            MonarchDragHandle(label = "Reorder", state = MonarchDragHandleState.Dropped, onMove = {})
            MonarchDragHandle(label = "Reorder", enabled = false)
        }
        Row {
            MonarchDragHandle(
                label = "Move tab",
                axis = MonarchDragHandleAxis.Horizontal,
                size = MonarchDragHandleSize.Sm,
            )
            MonarchDragHandle(label = "Move card", axis = MonarchDragHandleAxis.Both)
        }
    }
}
