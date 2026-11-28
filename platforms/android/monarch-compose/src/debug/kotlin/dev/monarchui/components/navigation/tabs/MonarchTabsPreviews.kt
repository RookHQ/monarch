package dev.monarchui.components.navigation.tabs

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchTabsPreview() {
    PreviewFrame {
        MonarchTabs(selectedIndex = 0, onSelectedIndexChange = {}) {
            tab("Board")
            tab("List")
            tab("Timeline", enabled = false)
        }
        MonarchTabs(selectedIndex = 0, onSelectedIndexChange = {}) {
            tab("Open", count = "6", countTone = MonarchTone.Accent)
            tab("Closed", count = "15")
        }
    }
}
