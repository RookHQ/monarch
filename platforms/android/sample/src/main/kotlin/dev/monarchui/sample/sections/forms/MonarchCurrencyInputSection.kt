package dev.monarchui.sample.sections.forms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.billing.money.MonarchMoneyCurrencyDisplay
import dev.monarchui.components.forms.currencyinput.MonarchCurrencyInput
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of CurrencyInput. */
object MonarchCurrencyInputSection : CataloguePage {
    override val title = "CurrencyInput"

    private const val START_AMOUNT = 19.99
    private const val MAX_AMOUNT = 500.0
    private const val START_CENTS = 1999L
    private const val START_YEN = 1500L

    @Composable
    override fun Content() {
        var amount by rememberSaveable { mutableStateOf<Double?>(START_AMOUNT) }
        var cents by rememberSaveable { mutableStateOf<Long?>(START_CENTS) }
        var yen by rememberSaveable { mutableStateOf<Long?>(START_YEN) }
        CatalogueCaption("Dollars, up to 500")
        MonarchCurrencyInput(
            value = amount,
            currencyCode = "USD",
            onValueChange = { amount = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Amount",
            helperText = "Charged today.",
            maximum = MAX_AMOUNT,
        )
        CatalogueCaption("Euros stored as cents")
        MonarchCurrencyInput(
            minorUnits = cents,
            currencyCode = "EUR",
            onMinorUnitsChange = { cents = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Price",
        )
        MonarchText(text = "Stored: ${cents ?: "nothing"}")
        CatalogueCaption("Yen with the code and steppers")
        MonarchCurrencyInput(
            minorUnits = yen,
            currencyCode = "JPY",
            onMinorUnitsChange = { yen = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Fee",
            currencyDisplay = MonarchMoneyCurrencyDisplay.Code,
            step = 100L,
            showSteppers = true,
        )
    }
}
