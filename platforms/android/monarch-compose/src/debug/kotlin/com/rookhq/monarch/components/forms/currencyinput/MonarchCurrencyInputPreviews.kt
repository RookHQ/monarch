package com.rookhq.monarch.components.forms.currencyinput

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.billing.money.MonarchMoneyCurrencyDisplay
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchCurrencyInputPreview() {
    PreviewFrame {
        MonarchCurrencyInput(value = 19.99, currencyCode = "USD", onValueChange = {}, label = "Amount")
        MonarchCurrencyInput(
            minorUnits = 1500L,
            currencyCode = "JPY",
            onMinorUnitsChange = {},
            label = "Price",
            currencyDisplay = MonarchMoneyCurrencyDisplay.Code,
            showSteppers = true,
        )
        MonarchCurrencyInput(
            value = -5.0,
            currencyCode = "EUR",
            onValueChange = {},
            label = "Refund",
            allowNegative = true,
        )
        MonarchCurrencyInput(value = null, currencyCode = "KWD", onValueChange = {}, label = "Fee", enabled = false)
    }
}
