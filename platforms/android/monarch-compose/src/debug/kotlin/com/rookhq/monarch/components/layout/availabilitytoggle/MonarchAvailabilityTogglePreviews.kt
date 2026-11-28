package com.rookhq.monarch.components.layout.availabilitytoggle

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAvailabilityTogglePreview() {
    PreviewFrame {
        MonarchAvailabilityToggle(available = true, onAvailableChange = {}, showHint = true)
        MonarchAvailabilityToggle(available = false, onAvailableChange = {}, showHint = true)
        MonarchAvailabilityToggle(available = false, onAvailableChange = {}, pending = true)
        MonarchAvailabilityToggle(available = true, onAvailableChange = {}, enabled = false)
    }
}
