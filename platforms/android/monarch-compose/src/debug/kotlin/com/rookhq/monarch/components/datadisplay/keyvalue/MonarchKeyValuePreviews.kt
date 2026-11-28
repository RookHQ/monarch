package com.rookhq.monarch.components.datadisplay.keyvalue

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
