package com.rookhq.monarch.components.search.savedviews

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val PreviewViews = listOf(
    MonarchSavedView("all", "All deals", count = 128, readOnly = true, group = "Built-in"),
    MonarchSavedView("open", "Open deals", count = 42, default = true, group = "Mine"),
    MonarchSavedView("q4", "Closing this quarter", shared = true, group = "Mine"),
)

@MonarchPreviews
@Composable
internal fun MonarchSavedViewsPreview() {
    PreviewFrame {
        MonarchSavedViews(views = PreviewViews, selectedId = "open", onSelect = {})
        MonarchSavedViews(views = PreviewViews, selectedId = "q4", onSelect = {}, modified = true)
        MonarchSavedViews(views = PreviewViews, selectedId = null, onSelect = {}, size = MonarchButtonSize.Sm)
        MonarchSavedViews(views = PreviewViews, selectedId = "all", onSelect = {}, enabled = false)
    }
}
