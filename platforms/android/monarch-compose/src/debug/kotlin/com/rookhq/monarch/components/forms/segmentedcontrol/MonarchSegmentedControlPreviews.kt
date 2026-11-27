package com.rookhq.monarch.components.forms.segmentedcontrol

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchSegmentedControlPreview() {
    PreviewFrame {
        MonarchSegmentedControl(listOf("List", "Board", "Grid"), selectedIndex = 0, onSelect = {})
        MonarchSegmentedControl(listOf("1 day", "7 days", "30 days"), selectedIndex = 1, onSelect = {})
    }
}
