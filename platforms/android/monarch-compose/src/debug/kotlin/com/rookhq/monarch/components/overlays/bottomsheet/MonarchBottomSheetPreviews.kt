package com.rookhq.monarch.components.overlays.bottomsheet

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchBottomSheetPreview() {
    PreviewFrame {
        MonarchBottomSheet(
            onDismissRequest = {},
            title = "Share report",
            description = "People with the link can view the Q3 revenue report.",
        ) {
            MonarchText(text = "Copy link")
        }
    }
}
