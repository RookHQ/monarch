package com.rookhq.monarch.sample.sections.geo

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.geo.countryselect.MonarchCountrySelect
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of CountrySelect. */
object MonarchCountrySelectSection : CataloguePage {
    override val title = "CountrySelect"

    @Composable
    override fun Content() {
        var shipping by rememberSaveable { mutableStateOf<String?>(null) }
        var market by rememberSaveable { mutableStateOf<String?>("IE") }
        CatalogueCaption("Every country, with suggestions first")
        MonarchCountrySelect(
            selected = shipping,
            onSelectedChange = { shipping = it },
            modifier = Modifier.fillMaxWidth(),
            priority = listOf("US", "GB", "DE"),
            helperText = "Where the order ships.",
        )
        CatalogueCaption("A short list in a menu, with flags")
        MonarchCountrySelect(
            selected = market,
            onSelectedChange = { market = it },
            modifier = Modifier.fillMaxWidth(),
            countries = listOf("GB", "IE", "FR", "DE"),
            showFlag = true,
        )
        CatalogueCaption("Disabled")
        MonarchCountrySelect(
            selected = "JP",
            onSelectedChange = {},
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
        )
    }
}
