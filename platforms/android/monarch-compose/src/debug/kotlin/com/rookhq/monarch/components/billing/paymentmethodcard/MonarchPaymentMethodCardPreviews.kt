package com.rookhq.monarch.components.billing.paymentmethodcard

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchPaymentMethodCardPreview() {
    PreviewFrame {
        MonarchPaymentMethodCard(
            brand = "visa",
            last4 = "4242",
            expiryMonth = 8,
            expiryYear = 29,
            holderName = "Asha Rao",
            default = true,
            onRemove = {},
        )
        MonarchPaymentMethodCard(
            brand = "mastercard",
            last4 = "5100",
            expiry = MonarchPaymentMethodCardExpiry.Expiring,
            onMakeDefault = {},
            onRemove = {},
        )
        MonarchPaymentMethodCard(
            brand = "amex",
            last4 = "0005",
            expiry = MonarchPaymentMethodCardExpiry.Expired,
            onMakeDefault = {},
            onRemove = {},
        )
    }
}
