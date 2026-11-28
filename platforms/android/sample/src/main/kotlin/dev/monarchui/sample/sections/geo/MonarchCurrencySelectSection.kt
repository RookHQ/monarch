package dev.monarchui.sample.sections.geo

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.geo.currencyselect.MonarchCurrencySelect
import dev.monarchui.components.geo.currencyselect.MonarchCurrencySelectDisplay
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of CurrencySelect. */
object MonarchCurrencySelectSection : CataloguePage {
    override val title = "CurrencySelect"

    @Composable
    override fun Content() {
        var billing by rememberSaveable { mutableStateOf<String?>("EUR") }
        var payout by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Every active currency, by name")
        MonarchCurrencySelect(
            selected = billing,
            onSelectedChange = { billing = it },
            modifier = Modifier.fillMaxWidth(),
            priority = listOf("USD", "EUR", "GBP"),
        )
        CatalogueCaption("The provider's currencies, by code and name")
        MonarchCurrencySelect(
            selected = payout,
            onSelectedChange = { payout = it },
            modifier = Modifier.fillMaxWidth(),
            currencies = listOf("USD", "EUR", "GBP", "INR"),
            display = MonarchCurrencySelectDisplay.CodeAndName,
            helperText = "Payouts are sent in this currency.",
        )
    }
}
