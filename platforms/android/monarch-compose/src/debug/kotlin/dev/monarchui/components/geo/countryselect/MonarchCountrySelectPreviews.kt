package dev.monarchui.components.geo.countryselect

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import java.util.Locale

@MonarchPreviews
@Composable
internal fun MonarchCountrySelectPreview() {
    PreviewFrame {
        MonarchCountrySelect(selected = "PT", onSelectedChange = {}, locale = Locale.US)
        MonarchCountrySelect(
            selected = null,
            onSelectedChange = {},
            locale = Locale.US,
            priority = listOf("US", "GB"),
            helperText = "Where the order ships.",
        )
        MonarchCountrySelect(
            selected = null,
            onSelectedChange = {},
            locale = Locale.US,
            errorText = "Choose a country.",
        )
        MonarchCountrySelect(selected = "DE", onSelectedChange = {}, locale = Locale.GERMANY, enabled = false)
    }
}
