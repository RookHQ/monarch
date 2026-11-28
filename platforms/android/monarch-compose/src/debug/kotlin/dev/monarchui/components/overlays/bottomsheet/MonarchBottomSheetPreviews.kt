package dev.monarchui.components.overlays.bottomsheet

import androidx.compose.runtime.Composable
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
