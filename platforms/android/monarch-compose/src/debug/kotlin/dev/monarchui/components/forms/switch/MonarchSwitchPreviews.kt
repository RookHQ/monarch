package dev.monarchui.components.forms.switch

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchSwitchPreview() {
    PreviewFrame {
        MonarchSwitch(checked = true, onCheckedChange = {}, label = "Notifications")
        MonarchSwitch(checked = false, onCheckedChange = {}, label = "Compact view", description = "Show more rows.")
        MonarchSwitch(checked = true, onCheckedChange = {}, label = "Managed by your admin", enabled = false)
    }
}
