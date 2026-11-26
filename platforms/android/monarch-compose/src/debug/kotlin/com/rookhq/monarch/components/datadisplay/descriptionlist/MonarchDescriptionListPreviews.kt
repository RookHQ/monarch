package com.rookhq.monarch.components.datadisplay.descriptionlist

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val previewItems = listOf(
    MonarchDescriptionItem("Owner", "Ada Lovelace"),
    MonarchDescriptionItem("Stage", "Negotiation", content = { MonarchBadge(text = "Negotiation") }),
    MonarchDescriptionItem("Customer ID", "cus_9s6XKzkNRiz8i3", copyable = true),
    MonarchDescriptionItem("Close date", null),
    MonarchDescriptionItem(
        "Notes",
        "Asked for a second call with the finance team before signing the annual contract.",
        maxLines = 1,
        wide = true,
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchDescriptionListPreview() {
    PreviewFrame {
        MonarchDescriptionList(items = previewItems)
        MonarchDescriptionList(items = previewItems.take(2), layout = MonarchDescriptionListLayout.Inline)
    }
}
