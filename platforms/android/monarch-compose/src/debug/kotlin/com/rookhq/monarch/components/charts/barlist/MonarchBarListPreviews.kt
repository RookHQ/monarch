package com.rookhq.monarch.components.charts.barlist

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val Pages = listOf(
    MonarchBarListItem("/pricing", 4210.0),
    MonarchBarListItem("/docs", 2980.0, description = "Documentation"),
    MonarchBarListItem("/blog", 1120.0, highlighted = true),
    MonarchBarListItem("/legacy", null),
)

@MonarchPreviews
@Composable
internal fun MonarchBarListPreview() {
    PreviewFrame {
        MonarchBarList(items = Pages, ranked = true, label = "Top pages")
        MonarchBarList(items = Pages, limit = 2, onSelect = {})
    }
}

@MonarchPreviews
@Composable
internal fun MonarchBarListStatesPreview() {
    PreviewFrame {
        MonarchBarList(items = emptyList())
        MonarchBarList(items = Pages, loading = true)
    }
}
