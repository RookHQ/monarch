package com.rookhq.monarch.components.datadisplay.accordion

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

private val previewItems = listOf(
    MonarchAccordionItem("billing", "How does billing work?"),
    MonarchAccordionItem("cancel", "Can I cancel at any time?"),
    MonarchAccordionItem("legacy", "Legacy plans", enabled = false),
)

@MonarchPreviews
@Composable
internal fun MonarchAccordionPreview() {
    PreviewFrame {
        MonarchAccordion(
            items = previewItems,
            expanded = setOf("billing"),
            onExpandedChange = {},
            trailingIcon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
        ) { item -> Text("The answer to ${item.title.lowercase()}") }
    }
}
