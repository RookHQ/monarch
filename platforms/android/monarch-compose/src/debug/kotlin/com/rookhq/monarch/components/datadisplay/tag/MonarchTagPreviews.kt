package com.rookhq.monarch.components.datadisplay.tag

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
