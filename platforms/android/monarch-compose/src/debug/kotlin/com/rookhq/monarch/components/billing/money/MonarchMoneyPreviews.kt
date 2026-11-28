package com.rookhq.monarch.components.billing.money

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import java.math.BigDecimal

@MonarchPreviews
@Composable
internal fun MonarchMoneyPreview() {
    PreviewFrame {
        MonarchMoney(amount = BigDecimal("1234.5"), currencyCode = "USD", style = MonarchTextStyle.Title)
        MonarchMoney(minorUnits = -4200L, currencyCode = "EUR", accountingSign = true, toneNegative = true)
        MonarchMoney(amount = BigDecimal("1300000"), currencyCode = "INR", notation = MonarchMoneyNotation.Compact)
        MonarchMoney(
            amount = BigDecimal("42"),
            currencyCode = "USD",
            signDisplay = MonarchMoneySignDisplay.ExceptZero,
            currencyDisplay = MonarchMoneyCurrencyDisplay.Name,
        )
        MonarchMoney(amount = null, currencyCode = "USD")
    }
}
