package com.rookhq.monarch.sample.sections.geo

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.geo.countryselect.MonarchCountrySelect
import com.rookhq.monarch.components.geo.regionselect.MonarchRegionSelect
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of RegionSelect. */
object MonarchRegionSelectSection : CataloguePage {
    override val title = "RegionSelect"

    @Composable
    override fun Content() {
        var country by rememberSaveable { mutableStateOf<String?>("US") }
        var region by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("The regions follow the country; a country without a list gets a text field")
        MonarchCountrySelect(
            selected = country,
            onSelectedChange = {
                country = it
                region = null
            },
            modifier = Modifier.fillMaxWidth(),
            priority = listOf("US", "CA", "AU", "GB", "IN"),
        )
        MonarchRegionSelect(
            country = country,
            selected = region,
            onSelectedChange = { region = it },
            modifier = Modifier.fillMaxWidth(),
            showCode = true,
        )
        CatalogueCaption("No country yet")
        MonarchRegionSelect(country = null, selected = null, onSelectedChange = {}, modifier = Modifier.fillMaxWidth())
    }
}
