package dev.monarchui.sample.sections.geo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.monarchui.components.geo.locationfields.MonarchLocationAdapter
import dev.monarchui.components.geo.locationfields.MonarchLocationCity
import dev.monarchui.components.geo.locationfields.MonarchLocationField
import dev.monarchui.components.geo.locationfields.MonarchLocationFields
import dev.monarchui.components.geo.locationfields.MonarchLocationValue
import dev.monarchui.components.geo.regionselect.MonarchRegionSelectRegion
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import kotlinx.coroutines.delay

private const val SampleLatencyMillis = 300L

private val SampleCities = listOf(
    MonarchLocationCity("us-ca-oakland", "Oakland", "California", MonarchRegionSelectRegion("US-CA", "California")),
    MonarchLocationCity("us-or-portland", "Portland", "Oregon", MonarchRegionSelectRegion("US-OR", "Oregon")),
    MonarchLocationCity("us-me-portland", "Portland", "Maine", MonarchRegionSelectRegion("US-ME", "Maine")),
    MonarchLocationCity("us-ca-pomona", "Pomona", "California", MonarchRegionSelectRegion("US-CA", "California")),
)

/** The catalogue page of LocationFields. */
object MonarchLocationFieldsSection : CataloguePage {
    override val title = "LocationFields"

    @Composable
    override fun Content() {
        var address by remember { mutableStateOf(MonarchLocationValue(country = "US")) }
        CatalogueCaption("Every part, with a city search (try “po”) over a sample adapter")
        MonarchLocationFields(
            value = address,
            onValueChange = { next, _ -> address = next },
            adapter = MonarchLocationAdapter(
                countries = listOf("US", "CA", "PT", "GB"),
                cities = { query ->
                    delay(SampleLatencyMillis)
                    SampleCities.filter { city ->
                        (query.region == null || city.region?.code == query.region) &&
                            city.name.startsWith(query.query, ignoreCase = true)
                    }
                },
            ),
            legend = "Shipping address",
        )
        CatalogueCaption("Typed region and city, with errors")
        var typed by remember { mutableStateOf(MonarchLocationValue(country = "PT")) }
        MonarchLocationFields(
            value = typed,
            onValueChange = { next, _ -> typed = next },
            fields = listOf(MonarchLocationField.Region, MonarchLocationField.City, MonarchLocationField.PostalCode),
            errors = if (typed.postalCode.isEmpty()) {
                mapOf(MonarchLocationField.PostalCode to "Enter a postal code.")
            } else {
                emptyMap()
            },
        )
        CatalogueCaption("Disabled")
        MonarchLocationFields(
            value = MonarchLocationValue(country = "CA", region = "CA-QC", regionName = "Quebec", city = "Montréal"),
            onValueChange = { _, _ -> },
            fields = listOf(MonarchLocationField.Region, MonarchLocationField.City),
            enabled = false,
        )
    }
}
