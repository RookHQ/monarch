package dev.monarchui.sample.sections.forms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.forms.numberinput.MonarchNumberInput
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of NumberInput. */
object MonarchNumberInputSection : CataloguePage {
    override val title = "NumberInput"

    private const val START_QUANTITY = 1.0
    private const val MAX_QUANTITY = 99.0
    private const val START_PRICE = 0.3
    private const val PRICE_STEP = 0.1
    private const val GUESTS = 4.0

    @Composable
    override fun Content() {
        var quantity by rememberSaveable { mutableStateOf<Double?>(START_QUANTITY) }
        var price by rememberSaveable { mutableStateOf<Double?>(START_PRICE) }
        CatalogueCaption("A quantity from 0 to 99")
        MonarchNumberInput(
            value = quantity,
            onValueChange = { quantity = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Quantity",
            helperText = "Up to 99 per order.",
            minimum = 0.0,
            maximum = MAX_QUANTITY,
        )
        CatalogueCaption("A decimal step without drift")
        MonarchNumberInput(
            value = price,
            onValueChange = { price = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Price",
            step = PRICE_STEP,
        )
        CatalogueCaption("Disabled")
        MonarchNumberInput(
            value = GUESTS,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = "Guests",
            enabled = false,
        )
    }
}
