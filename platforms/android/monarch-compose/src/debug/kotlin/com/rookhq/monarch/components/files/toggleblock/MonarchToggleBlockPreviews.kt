package com.rookhq.monarch.components.files.toggleblock

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
