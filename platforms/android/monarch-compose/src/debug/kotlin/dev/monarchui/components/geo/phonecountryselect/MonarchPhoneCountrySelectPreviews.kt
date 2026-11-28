package dev.monarchui.components.geo.phonecountryselect

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
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
