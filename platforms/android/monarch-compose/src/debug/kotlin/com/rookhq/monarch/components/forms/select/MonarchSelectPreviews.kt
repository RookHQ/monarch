package com.rookhq.monarch.components.forms.select

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val previewOptions = listOf(
    MonarchSelectOption("eu", "Europe (Frankfurt)", group = "Recommended"),
    MonarchSelectOption("us", "United States (Virginia)", description = "Lowest latency in North America."),
    MonarchSelectOption("ap", "Asia Pacific (Singapore)", enabled = false),
)

@MonarchPreviews
@Composable
internal fun MonarchSelectPreview() {
    PreviewFrame {
        MonarchSelect(options = previewOptions, selected = "eu", onSelect = {}, label = "Region")
        MonarchSelect(previewOptions, selected = null, onSelect = {}, label = "Region", helperText = "Help.")
        MonarchSelect(previewOptions, selected = null, onSelect = {}, label = "Region", errorText = "Choose a region.")
        MonarchSelect(options = previewOptions, selected = "us", onSelect = {}, label = "Region", enabled = false)
    }
}
