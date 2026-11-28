package com.rookhq.monarch.components.identity.spaceswitcher

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val PreviewSpaces = listOf(
    MonarchSpaceOption("eng", "Engineering", "18 members"),
    MonarchSpaceOption("mkt", "Marketing", "9 members"),
)

@MonarchPreviews
@Composable
internal fun MonarchSpaceSwitcherPreview() {
    PreviewFrame {
        MonarchSpaceSwitcher(spaces = PreviewSpaces, selection = null, onSelectionChange = {})
        MonarchSpaceSwitcher(spaces = PreviewSpaces, selection = "eng", onSelectionChange = {})
        MonarchSpaceSwitcher(spaces = PreviewSpaces, selection = "mkt", onSelectionChange = {}, iconOnly = true)
        MonarchSpaceSwitcher(spaces = PreviewSpaces, selection = null, onSelectionChange = {}, allowsAll = false)
    }
}
