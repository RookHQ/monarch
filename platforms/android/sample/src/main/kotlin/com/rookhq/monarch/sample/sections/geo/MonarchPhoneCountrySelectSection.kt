package com.rookhq.monarch.sample.sections.geo

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.geo.phonecountryselect.MonarchPhoneCountrySelect
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of PhoneCountrySelect. */
object MonarchPhoneCountrySelectSection : CataloguePage {
    override val title = "PhoneCountrySelect"

    @Composable
    override fun Content() {
        var country by rememberSaveable { mutableStateOf<String?>("GB") }
        var dialCode by rememberSaveable { mutableStateOf<String?>("+44") }
        var number by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("A calling code above the number")
        MonarchPhoneCountrySelect(
            selected = country,
            onSelectedChange = { code, dial ->
                country = code
                dialCode = dial
            },
            modifier = Modifier.fillMaxWidth(),
            preferred = listOf("GB", "US"),
        )
        MonarchTextField(
            value = number,
            onValueChange = { number = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Phone number",
            helperText = dialCode?.let { "Dialled as $it $number" },
        )
    }
}
