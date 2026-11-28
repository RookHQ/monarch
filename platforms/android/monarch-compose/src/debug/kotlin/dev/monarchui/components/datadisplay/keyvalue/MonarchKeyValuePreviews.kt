package dev.monarchui.components.datadisplay.keyvalue

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

private val previewItems = listOf(
    MonarchKeyValueItem("Region", "eu-west"),
    MonarchKeyValueItem("Plan", "Team"),
    MonarchKeyValueItem("ID", "4f2a…", copyValue = "4f2a9c1e-77b0", monospace = true),
    MonarchKeyValueItem("Owner", null),
)

@MonarchPreviews
@Composable
internal fun MonarchKeyValuePreview() {
    PreviewFrame {
        MonarchKeyValueSeparator.entries.forEach { MonarchKeyValue(items = previewItems, separator = it) }
        MonarchKeyValue(items = previewItems, size = MonarchKeyValueSize.Md)
        MonarchKeyValue(
            items = previewItems,
            layout = MonarchKeyValueLayout.Stack,
            copyIcon = { MonarchIcon(PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm) },
        )
    }
}
