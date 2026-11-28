package com.rookhq.monarch.components.geo.countryselect

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
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
