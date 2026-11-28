package com.rookhq.monarch.components.forms.switch

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchSwitchPreview() {
    PreviewFrame {
        MonarchSwitch(checked = true, onCheckedChange = {}, label = "Notifications")
        MonarchSwitch(checked = false, onCheckedChange = {}, label = "Compact view", description = "Show more rows.")
        MonarchSwitch(checked = true, onCheckedChange = {}, label = "Managed by your admin", enabled = false)
    }
}
