package dev.monarchui.sample.sections.forms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.forms.phoneinput.MonarchPhoneInput
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of PhoneInput. */
object MonarchPhoneInputSection : CataloguePage {
    override val title = "PhoneInput"

    @Composable
    override fun Content() {
        var phone by rememberSaveable { mutableStateOf<String?>("+442079460958") }
        var mobile by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("With suggested countries")
        MonarchPhoneInput(
            value = phone,
            onValueChange = { phone = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Phone",
            helperText = "Stored as ${phone ?: "nothing yet"}",
            preferred = listOf("GB", "US", "IN"),
        )
        CatalogueCaption("Limited countries")
        MonarchPhoneInput(
            value = mobile,
            onValueChange = { mobile = it },
            modifier = Modifier.fillMaxWidth(),
            defaultCountry = "IN",
            label = "Mobile",
            countries = listOf("IN", "AE", "SG"),
        )
        CatalogueCaption("Disabled")
        MonarchPhoneInput(
            value = "+15550100123",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = "Office phone",
            enabled = false,
        )
    }
}
