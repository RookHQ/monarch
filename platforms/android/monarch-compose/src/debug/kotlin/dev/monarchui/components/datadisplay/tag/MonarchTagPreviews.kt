package dev.monarchui.components.datadisplay.tag

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchTagReadOnlyPreview() {
    PreviewFrame {
        MonarchTagSize.entries.forEach { size ->
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTagTokens.ContentGap)) {
                MonarchTone.entries.forEach { tone -> MonarchTag(text = tone.name, tone = tone, size = size) }
            }
        }
    }
}

@MonarchPreviews
@Composable
internal fun MonarchTagInteractivePreview() {
    PreviewFrame {
        MonarchTag(
            text = "Design",
            onRemove = {},
            removeIcon = {
                MonarchIcon(imageVector = PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm)
            },
            tone = MonarchTone.Info,
        )
        MonarchTag(text = "Open", selected = true, onSelectedChange = {})
        MonarchTag(text = "Closed", selected = false, onSelectedChange = {})
        MonarchTag(text = "Archived", selected = false, onSelectedChange = {}, enabled = false)
    }
}
