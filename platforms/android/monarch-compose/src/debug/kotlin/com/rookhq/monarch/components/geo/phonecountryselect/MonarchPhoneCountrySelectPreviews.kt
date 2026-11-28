package com.rookhq.monarch.components.geo.phonecountryselect

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import java.util.Locale

@MonarchPreviews
@Composable
internal fun MonarchPhoneCountrySelectPreview() {
    PreviewFrame {
        MonarchPhoneCountrySelect(selected = "PT", onSelectedChange = { _, _ -> }, locale = Locale.US)
        MonarchPhoneCountrySelect(
            selected = null,
            onSelectedChange = { _, _ -> },
            locale = Locale.US,
            preferred = listOf("GB", "US"),
            errorText = "Choose a country code.",
        )
        MonarchPhoneCountrySelect(selected = "GB", onSelectedChange = { _, _ -> }, locale = Locale.US, enabled = false)
    }
}
