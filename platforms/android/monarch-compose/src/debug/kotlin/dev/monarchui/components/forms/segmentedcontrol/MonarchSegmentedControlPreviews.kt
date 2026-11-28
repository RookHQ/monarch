package dev.monarchui.components.forms.segmentedcontrol

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchSegmentedControlPreview() {
    PreviewFrame {
        MonarchSegmentedControl(listOf("List", "Board", "Grid"), selectedIndex = 0, onSelect = {})
        MonarchSegmentedControl(listOf("1 day", "7 days", "30 days"), selectedIndex = 1, onSelect = {})
    }
}
