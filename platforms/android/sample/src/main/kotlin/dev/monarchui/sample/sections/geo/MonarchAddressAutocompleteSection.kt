@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.geo

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.geo.addressautocomplete.MonarchAddressAutocomplete
import dev.monarchui.components.geo.addressautocomplete.MonarchAddressAutocompleteAddress
import dev.monarchui.components.geo.addressautocomplete.MonarchAddressAutocompleteSuggestion
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import kotlinx.coroutines.delay

/** The catalogue page of AddressAutocomplete. */
object MonarchAddressAutocompleteSection : CataloguePage {
    override val title = "AddressAutocomplete"

    private const val NETWORK_DELAY_MILLIS = 400L

    /** Sample places standing in for the app's provider. */
    private val places = listOf(
        MonarchAddressAutocompleteSuggestion(
            id = "lisbon",
            label = "10 Rua Augusta",
            description = "Lisbon, Portugal",
            address = MonarchAddressAutocompleteAddress(
                line1 = "10 Rua Augusta",
                city = "Lisbon",
                postalCode = "1100-053",
                countryCode = "PT",
            ),
        ),
        MonarchAddressAutocompleteSuggestion("porto", "5 Rua das Flores", "Porto, Portugal"),
        MonarchAddressAutocompleteSuggestion("leeds", "1 Park Row", "Leeds, United Kingdom"),
    )

    @Composable
    override fun Content() {
        var shipping by remember { mutableStateOf(MonarchAddressAutocompleteAddress()) }
        CatalogueCaption("Suggestions from a sample provider, then the form for review")
        MonarchAddressAutocomplete(
            address = shipping,
            onAddressChange = { shipping = it },
            search = { query ->
                delay(NETWORK_DELAY_MILLIS)
                places.filter { place ->
                    "${place.label} ${place.description}".contains(query, ignoreCase = true)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            resolve = { suggestion ->
                delay(NETWORK_DELAY_MILLIS)
                val city = suggestion.description.orEmpty().substringBefore(",")
                MonarchAddressAutocompleteAddress(line1 = suggestion.label, city = city)
            },
        )
    }
}
