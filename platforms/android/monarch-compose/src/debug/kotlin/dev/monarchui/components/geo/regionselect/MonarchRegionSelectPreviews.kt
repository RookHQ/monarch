package dev.monarchui.components.geo.regionselect

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
