package dev.monarchui.components.files.toggleblock

import androidx.compose.runtime.Composable
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchToggleBlockPreview() {
    PreviewFrame {
        MonarchToggleBlock(summary = "Why some records are skipped") {
            MonarchText(text = "Records changed while the import runs are picked up the next night.")
        }
        MonarchToggleBlock(
            summary = "Details",
            initiallyExpanded = true,
            trailingIcon = {
                MonarchIcon(imageVector = PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm)
            },
        ) {
            MonarchText(text = "Shown while expanded.")
        }
        MonarchToggleBlock(summary = "Disabled", enabled = false) { MonarchText(text = "Hidden") }
    }
}
