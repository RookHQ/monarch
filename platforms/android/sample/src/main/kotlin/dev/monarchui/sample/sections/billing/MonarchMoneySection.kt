package dev.monarchui.sample.sections.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.billing.money.MonarchMoney
import dev.monarchui.components.billing.money.MonarchMoneyCurrencyDisplay
import dev.monarchui.components.billing.money.MonarchMoneyNotation
import dev.monarchui.components.billing.money.MonarchMoneySignDisplay
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import java.math.BigDecimal

/** The catalogue page of Money: amounts written by the platform's currency formats. */
object MonarchMoneySection : CataloguePage {
    override val title = "Money"

    private const val PRICE_CENTS = 1999L
    private const val REFUND_CENTS = -4200L
    private val revenue = BigDecimal("1284000")
    private val gain = BigDecimal("42")

    @Composable
    override fun Content() {
        var compact by rememberSaveable { mutableStateOf(true) }
        CatalogueCaption("Minor units, a headline and a missing amount")
        MonarchMoney(minorUnits = PRICE_CENTS, currencyCode = "USD", style = MonarchTextStyle.Title)
        MonarchMoney(amount = null, currencyCode = "USD")
        CatalogueCaption("Signs: a refund in accounting form, a gain with a plus sign")
        MonarchMoney(minorUnits = REFUND_CENTS, currencyCode = "EUR", accountingSign = true, toneNegative = true)
        MonarchMoney(amount = gain, currencyCode = "USD", signDisplay = MonarchMoneySignDisplay.ExceptZero)
        CatalogueCaption("Compact notation, read in full by TalkBack")
        MonarchSwitchRow(checked = compact, onCheckedChange = { compact = it }, label = "Compact")
        MonarchMoney(
            amount = revenue,
            currencyCode = "INR",
            notation = if (compact) MonarchMoneyNotation.Compact else MonarchMoneyNotation.Standard,
            style = MonarchTextStyle.Heading,
        )
        CatalogueCaption("Currency as code and as name")
        MonarchMoney(minorUnits = PRICE_CENTS, currencyCode = "USD", currencyDisplay = MonarchMoneyCurrencyDisplay.Code)
        MonarchMoney(minorUnits = PRICE_CENTS, currencyCode = "USD", currencyDisplay = MonarchMoneyCurrencyDisplay.Name)
    }
}
