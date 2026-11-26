package com.rookhq.monarch.sample.sections.forms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of Select. */
object MonarchSelectSection : CataloguePage {
    override val title = "Select"

    private val regions = listOf(
        MonarchSelectOption("eu", "Europe (Frankfurt)", group = "Recommended"),
        MonarchSelectOption("us", "United States (Virginia)", group = "Recommended"),
        MonarchSelectOption("ap", "Asia Pacific (Singapore)", description = "Lowest latency in Asia.", group = "Other"),
        MonarchSelectOption("sa", "South America (São Paulo)", enabled = false, group = "Other"),
    )

    private val sizes = listOf(
        MonarchSelectOption("s", "Small"),
        MonarchSelectOption("m", "Medium"),
        MonarchSelectOption("l", "Large"),
    )

    @Composable
    override fun Content() {
        var region by rememberSaveable { mutableStateOf<String?>(null) }
        var size by rememberSaveable { mutableStateOf<String?>("m") }
        CatalogueCaption("Groups, a description and a disabled option")
        MonarchSelect(
            options = regions,
            selected = region,
            onSelect = { region = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Region",
            helperText = "Where your data is stored.",
            errorText = if (region == null) "Choose a region." else null,
        )
        CatalogueCaption("A short list")
        MonarchSelect(
            options = sizes,
            selected = size,
            onSelect = { size = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Size",
        )
        CatalogueCaption("Disabled")
        MonarchSelect(
            options = sizes,
            selected = "l",
            onSelect = {},
            modifier = Modifier.fillMaxWidth(),
            label = "Size",
            enabled = false,
        )
    }
}
