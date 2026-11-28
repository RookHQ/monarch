package dev.monarchui.components.geo.currencyselect

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import java.util.Locale

@MonarchPreviews
@Composable
internal fun MonarchCurrencySelectPreview() {
    PreviewFrame {
        MonarchCurrencySelect(selected = "EUR", onSelectedChange = {}, locale = Locale.US)
        MonarchCurrencySelect(
            selected = "USD",
            onSelectedChange = {},
            locale = Locale.US,
            display = MonarchCurrencySelectDisplay.CodeAndName,
        )
        MonarchCurrencySelect(
            selected = null,
            onSelectedChange = {},
            locale = Locale.US,
            errorText = "Choose a currency.",
        )
        MonarchCurrencySelect(selected = "JPY", onSelectedChange = {}, locale = Locale.US, enabled = false)
    }
}
