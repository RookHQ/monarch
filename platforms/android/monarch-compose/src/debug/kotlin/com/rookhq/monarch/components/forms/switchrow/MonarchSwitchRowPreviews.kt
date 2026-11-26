package com.rookhq.monarch.components.forms.switchrow

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchSwitchRowPreview() {
    PreviewFrame {
        MonarchSwitchRow(checked = true, onCheckedChange = {}, label = "Notifications")
        MonarchSwitchRow(
            checked = false,
            onCheckedChange = {},
            label = "Sync over mobile data",
            description = "May use your data allowance.",
        )
    }
}
