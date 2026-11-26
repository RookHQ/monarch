package com.rookhq.monarch.components.geo.regionselect

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchRegionSelectPreview() {
    PreviewFrame {
        MonarchRegionSelect(country = "US", selected = "US-CA", onSelectedChange = {})
        MonarchRegionSelect(country = "CA", selected = null, onSelectedChange = {}, showCode = true)
        MonarchRegionSelect(country = "PT", selected = "Lisboa", onSelectedChange = {})
        MonarchRegionSelect(country = null, selected = null, onSelectedChange = {})
    }
}
