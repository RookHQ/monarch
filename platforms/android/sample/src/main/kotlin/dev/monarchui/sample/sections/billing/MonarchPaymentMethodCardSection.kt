package dev.monarchui.sample.sections.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.billing.paymentmethodcard.MonarchPaymentMethodCard
import dev.monarchui.components.billing.paymentmethodcard.MonarchPaymentMethodCardExpiry
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of PaymentMethodCard. The actions wait a moment, and can be made to fail. */
object MonarchPaymentMethodCardSection : CataloguePage {
    override val title = "PaymentMethodCard"

    private const val WORK_MILLIS = 1200L
    private const val VISA_MONTH = 8
    private const val VISA_YEAR = 29

    @Composable
    override fun Content() {
        var defaultCard by rememberSaveable { mutableStateOf("visa") }
        var removed by rememberSaveable { mutableStateOf(false) }
        var fail by rememberSaveable { mutableStateOf(false) }
        MonarchSwitchRow(checked = fail, onCheckedChange = { fail = it }, label = "Make the actions fail")
        CatalogueCaption("Saved cards")
        MonarchPaymentMethodCard(
            brand = "visa",
            last4 = "4242",
            expiryMonth = VISA_MONTH,
            expiryYear = VISA_YEAR,
            holderName = "Asha Rao",
            default = defaultCard == "visa",
            removeEnabled = defaultCard != "visa",
            onMakeDefault = {
                delay(WORK_MILLIS)
                check(!fail) { "declined" }
                defaultCard = "visa"
            },
            onRemove = {},
        )
        if (!removed) {
            MonarchPaymentMethodCard(
                brand = "mastercard",
                last4 = "5100",
                expiry = MonarchPaymentMethodCardExpiry.Expiring,
                default = defaultCard == "mastercard",
                onMakeDefault = {
                    delay(WORK_MILLIS)
                    check(!fail) { "declined" }
                    defaultCard = "mastercard"
                },
                onRemove = {
                    delay(WORK_MILLIS)
                    check(!fail) { "declined" }
                    removed = true
                },
            )
        }
        CatalogueCaption("Expired: it cannot become the default")
        MonarchPaymentMethodCard(
            brand = "amex",
            last4 = "0005",
            expiry = MonarchPaymentMethodCardExpiry.Expired,
            onMakeDefault = {},
            onRemove = {},
        )
        MonarchText(text = "Default: $defaultCard", color = MonarchTheme.colors.textMuted)
    }
}
