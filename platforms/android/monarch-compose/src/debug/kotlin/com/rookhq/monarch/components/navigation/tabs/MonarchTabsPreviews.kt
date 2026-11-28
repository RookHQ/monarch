package com.rookhq.monarch.components.navigation.tabs

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
