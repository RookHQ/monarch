package dev.monarchui.components.collaboration.mentionpicker

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewQuery = MonarchMentionQuery(trigger = '@', text = "an", start = 6, end = 9)

private val PreviewItems = listOf(
    MonarchMentionItem(id = "u1", label = "Ana Lima", detail = "Product designer"),
    MonarchMentionItem(id = "u2", label = "Anders Berg", detail = "No access", enabled = false),
    MonarchMentionItem(id = "c1", label = "announcements", kind = MonarchMentionKind.Channel, detail = "214 members"),
)

@MonarchPreviews
@Composable
internal fun MonarchMentionPickerPreview() {
    PreviewFrame {
        MonarchMentionPicker(query = PreviewQuery, items = PreviewItems, onSelect = {})
        MonarchMentionPicker(query = PreviewQuery, items = emptyList(), onSelect = {})
        MonarchMentionPicker(
            query = PreviewQuery,
            items = emptyList(),
            onSelect = {},
            status = MonarchMentionPickerStatus.Failed,
            onRetry = {},
        )
    }
}
