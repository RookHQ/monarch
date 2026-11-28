package dev.monarchui.components.geo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.monarchui.components.geo.locationfields.MonarchLocationAdapter
import dev.monarchui.components.geo.locationfields.MonarchLocationCity
import dev.monarchui.components.geo.locationfields.MonarchLocationCityQuery
import dev.monarchui.components.geo.locationfields.MonarchLocationField
import dev.monarchui.components.geo.locationfields.MonarchLocationFields
import dev.monarchui.components.geo.locationfields.MonarchLocationFieldsLabels
import dev.monarchui.components.geo.locationfields.MonarchLocationFieldsTokens
import dev.monarchui.components.geo.locationfields.MonarchLocationValue
import dev.monarchui.components.geo.regionselect.MonarchRegionSelectRegion
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val California = MonarchRegionSelectRegion("US-CA", "California")
private val Oregon = MonarchRegionSelectRegion("US-OR", "Oregon")

private val Portland = MonarchLocationCity("portland-or", "Portland", description = "Oregon", region = Oregon)
private val Pomona = MonarchLocationCity("pomona-ca", "Pomona", description = "California", region = California)

private val CityAdapter = MonarchLocationAdapter(cities = { listOf(Portland, Pomona) })

private val Filled = MonarchLocationValue(
    country = "US",
    region = "US-CA",
    regionName = "California",
    city = "Oakland",
    cityId = "oakland-ca",
    postalCode = "94612",
)

internal object MonarchLocationFieldsFixtures : ComponentFixtures {
    override val id = "location-fields"
    override val fixtures = listOf(
        fixture("empty country and region") {
            MonarchLocationFields(
                value = MonarchLocationValue(),
                onValueChange = { _, _ -> },
                fields = listOf(MonarchLocationField.Country, MonarchLocationField.Region),
                locale = Locale.US,
            )
        },
        fixture("filled country and region, with a legend") {
            MonarchLocationFields(
                value = Filled,
                onValueChange = { _, _ -> },
                fields = listOf(MonarchLocationField.Country, MonarchLocationField.Region),
                legend = "Shipping address",
                locale = Locale.US,
            )
        },
        fixture("city search and postal code") {
            MonarchLocationFields(
                value = Filled,
                onValueChange = { _, _ -> },
                adapter = CityAdapter,
                fields = listOf(MonarchLocationField.City, MonarchLocationField.PostalCode),
            )
        },
        fixture("typed region and city") {
            MonarchLocationFields(
                value = MonarchLocationValue(country = "PT", regionName = "Lisboa", city = "Sintra"),
                onValueChange = { _, _ -> },
                fields = listOf(MonarchLocationField.Region, MonarchLocationField.City),
            )
        },
        fixture("no country yet") {
            MonarchLocationFields(
                value = MonarchLocationValue(),
                onValueChange = { _, _ -> },
                adapter = CityAdapter,
                fields = listOf(MonarchLocationField.Region, MonarchLocationField.City),
            )
        },
        fixture("errors") {
            MonarchLocationFields(
                value = MonarchLocationValue(country = "US"),
                onValueChange = { _, _ -> },
                fields = listOf(MonarchLocationField.City, MonarchLocationField.PostalCode),
                errors = mapOf(
                    MonarchLocationField.City to "Enter a city.",
                    MonarchLocationField.PostalCode to "Enter a postal code.",
                ),
            )
        },
        fixture("disabled") {
            MonarchLocationFields(
                value = Filled,
                onValueChange = { _, _ -> },
                fields = listOf(MonarchLocationField.Region, MonarchLocationField.PostalCode),
                enabled = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchLocationFieldsTest {
    @get:Rule
    val rule = createComposeRule()

    private fun row(text: String) = hasText(text) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)

    @Test
    fun aNewCountryClearsEveryOtherPart() {
        assertEquals(MonarchLocationValue(country = "CA"), MonarchLocationFieldsTokens.countryChanged(Filled, "CA"))
        assertEquals(MonarchLocationValue(), MonarchLocationFieldsTokens.countryChanged(Filled, null))
        assertNull(MonarchLocationFieldsTokens.countryChanged(Filled, "US"))
    }

    @Test
    fun aNewRegionClearsTheCityAndTypedTextIsKeptAsAName() {
        val list = listOf(California, Oregon)
        val oregon = MonarchLocationFieldsTokens.regionChanged(Filled, "US-OR", list)
        assertEquals(Filled.copy(region = "US-OR", regionName = "Oregon", city = "", cityId = null), oregon)
        assertEquals(Filled, MonarchLocationFieldsTokens.regionChanged(Filled, "US-CA", list))
        val typed = MonarchLocationFieldsTokens.regionChanged(MonarchLocationValue(country = "PT"), "Lisboa", null)
        assertEquals(MonarchLocationValue(country = "PT", regionName = "Lisboa"), typed)
        assertEquals("Lisboa", MonarchLocationFieldsTokens.regionSelection(typed))
        assertEquals("US-CA", MonarchLocationFieldsTokens.regionSelection(Filled))
        assertNull(MonarchLocationFieldsTokens.regionSelection(MonarchLocationValue()))
    }

    @Test
    fun aChosenCityFillsAnEmptyRegionAndATypedCityHasNoId() {
        val start = MonarchLocationValue(country = "US")
        val chosen = MonarchLocationFieldsTokens.cityChosen(start, "portland-or", listOf(Portland))
        assertEquals(
            MonarchLocationValue(
                country = "US",
                region = "US-OR",
                regionName = "Oregon",
                city = "Portland",
                cityId = "portland-or",
            ),
            chosen,
        )
        // A region already chosen is kept.
        assertEquals("US-CA", MonarchLocationFieldsTokens.cityChosen(Filled, "portland-or", listOf(Portland))?.region)
        val typedId = MonarchLocationFieldsTokens.typedOption(" Smallville ")?.id
        val typed = MonarchLocationFieldsTokens.cityChosen(start, typedId, emptyList())
        assertEquals(start.copy(city = "Smallville"), typed)
        assertEquals(start, MonarchLocationFieldsTokens.cityChosen(start.copy(city = "X"), null, emptyList()))
        assertNull(MonarchLocationFieldsTokens.cityChosen(start, "unknown", listOf(Portland)))
        assertNull(MonarchLocationFieldsTokens.typedOption("   "))
    }

    @Test
    fun theChosenCityStaysAnOptionWhenItIsNotInTheResults() {
        val options = MonarchLocationFieldsTokens.cityOptions(listOf(Portland), Filled)
        assertEquals(listOf("portland-or", "oakland-ca"), options.map { it.id })
        assertEquals("Oakland", options.last().label)
        val typed = MonarchLocationValue(country = "US", city = "Smallville")
        assertEquals("Smallville", MonarchLocationFieldsTokens.cityOptions(emptyList(), typed).single().label)
        assertEquals(emptyList(), MonarchLocationFieldsTokens.cityOptions(emptyList(), MonarchLocationValue()))
    }

    @Test
    fun citiesNeedTwoLettersOnlyWithoutARegion() {
        assertEquals(true, MonarchLocationFieldsTokens.needsMoreLetters(" p ", null, 2))
        assertEquals(false, MonarchLocationFieldsTokens.needsMoreLetters("po", null, 2))
        assertEquals(false, MonarchLocationFieldsTokens.needsMoreLetters("", "US-CA", 2))
    }

    @Test
    fun citiesAreSearchedAndAChosenCityFillsTheRegion() {
        val queries = mutableListOf<MonarchLocationCityQuery>()
        var value by mutableStateOf(MonarchLocationValue(country = "US"))
        var changed: MonarchLocationField? = null
        rule.setMonarchContent {
            MonarchLocationFields(
                value = value,
                onValueChange = { next, field ->
                    value = next
                    changed = field
                },
                adapter = MonarchLocationAdapter(
                    cities = { query ->
                        queries += query
                        listOf(Portland, Pomona).filter { it.name.startsWith(query.query, ignoreCase = true) }
                    },
                ),
                fields = listOf(MonarchLocationField.City),
                cityDebounceMillis = 0L,
            )
        }
        rule.onNodeWithContentDescription("City").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextInput("p")
        rule.waitForIdle()
        rule.onNodeWithText("Type at least 2 letters to search.").assertExists()
        assertEquals(emptyList(), queries)
        rule.onNode(hasSetTextAction()).performTextInput("or")
        rule.waitForIdle()
        assertEquals(MonarchLocationCityQuery("US", null, "por"), queries.last())
        rule.onNode(row("Portland")).performClick()
        rule.waitForIdle()
        assertEquals(MonarchLocationField.City, changed)
        assertEquals("portland-or", value.cityId)
        assertEquals("US-OR", value.region)
        assertEquals("Oregon", value.regionName)
    }

    @Test
    fun aCityNotInTheListCanBeKeptAsTyped() {
        var value by mutableStateOf(MonarchLocationValue(country = "US"))
        rule.setMonarchContent {
            MonarchLocationFields(
                value = value,
                onValueChange = { next, _ -> value = next },
                adapter = MonarchLocationAdapter(cities = { emptyList() }),
                fields = listOf(MonarchLocationField.City),
                cityDebounceMillis = 0L,
                labels = MonarchLocationFieldsLabels(noMatch = "Nothing found"),
            )
        }
        rule.onNodeWithContentDescription("City").performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextInput("Smallville")
        rule.waitForIdle()
        rule.onNodeWithText("Use “Smallville”").performClick()
        rule.waitForIdle()
        assertEquals("Smallville", value.city)
        assertNull(value.cityId)
    }

    @Test
    fun aFailedSearchSaysSoAndTypingAgainRetries() {
        var fail = true
        var calls = 0
        rule.setMonarchContent {
            MonarchLocationFields(
                value = MonarchLocationValue(country = "US"),
                onValueChange = { _, _ -> },
                adapter = MonarchLocationAdapter(
                    cities = {
                        calls++
                        if (fail) error("offline")
                        listOf(Portland)
                    },
                ),
                fields = listOf(MonarchLocationField.City),
                cityDebounceMillis = 0L,
                allowCustomCity = false,
            )
        }
        rule.onNodeWithContentDescription("City").performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextInput("po")
        rule.waitForIdle()
        rule.onNodeWithText("Couldn’t load cities. Type again to retry.").assertExists()
        fail = false
        rule.onNode(hasSetTextAction()).performTextInput("r")
        rule.waitForIdle()
        assertEquals(2, calls)
        rule.onNode(row("Portland")).assertExists()
    }

    @Test
    fun withoutACountryTheCitySearchIsDisabledAndSaysWhy() {
        rule.setMonarchContent {
            MonarchLocationFields(
                value = MonarchLocationValue(),
                onValueChange = { _, _ -> },
                adapter = CityAdapter,
                fields = listOf(MonarchLocationField.City),
            )
        }
        rule.onNodeWithContentDescription("City").assertIsNotEnabled()
        rule.onNodeWithText("Choose a country first").assertExists()
    }

    @Test
    fun aCityWithoutASourceAndThePostalCodeAreTextFields() {
        var value by mutableStateOf(MonarchLocationValue(country = "PT"))
        val changes = mutableListOf<MonarchLocationField>()
        rule.setMonarchContent {
            MonarchLocationFields(
                value = value,
                onValueChange = { next, field ->
                    value = next
                    changes += field
                },
                fields = listOf(MonarchLocationField.City, MonarchLocationField.PostalCode),
                labels = MonarchLocationFieldsLabels(postalCode = "Código postal"),
            )
        }
        rule.onNode(hasSetTextAction() and hasContentDescription("City")).assertTouchTargetAtLeast48dp(rule.density)
            .performTextInput("Sintra")
        rule.onNode(hasSetTextAction() and hasContentDescription("Código postal")).performTextInput("2710-405")
        rule.waitForIdle()
        assertEquals(MonarchLocationValue(country = "PT", city = "Sintra", postalCode = "2710-405"), value)
        assertEquals(listOf(MonarchLocationField.City, MonarchLocationField.PostalCode), changes)
    }

    @Test
    fun aCountryWithoutAListTakesATypedRegion() {
        var value by mutableStateOf(MonarchLocationValue(country = "PT", city = "Sintra"))
        rule.setMonarchContent {
            MonarchLocationFields(
                value = value,
                onValueChange = { next, _ -> value = next },
                fields = listOf(MonarchLocationField.Region),
            )
        }
        rule.onNode(hasSetTextAction()).performTextInput("Lisboa")
        rule.waitForIdle()
        assertEquals(MonarchLocationValue(country = "PT", regionName = "Lisboa"), value)
    }

    @Test
    fun theAdapterNamesTheRegionsAndLoadsThem() {
        var value by mutableStateOf(MonarchLocationValue(country = "AE"))
        rule.setMonarchContent {
            MonarchLocationFields(
                value = value,
                onValueChange = { next, _ -> value = next },
                adapter = MonarchLocationAdapter(
                    regions = { listOf(MonarchRegionSelectRegion("AE-DU", "Dubai")) },
                    regionLabel = { if (it == "AE") "Emirate" else null },
                ),
                fields = listOf(MonarchLocationField.Region),
            )
        }
        rule.onNodeWithContentDescription("Emirate").performClick()
        rule.waitForIdle()
        rule.onNode(row("Dubai")).performClick()
        rule.waitForIdle()
        assertEquals(MonarchLocationValue(country = "AE", region = "AE-DU", regionName = "Dubai"), value)
    }

    @Test
    fun theLegendIsAHeadingAndErrorsAreShown() {
        rule.setMonarchContent {
            MonarchLocationFields(
                value = MonarchLocationValue(country = "US"),
                onValueChange = { _, _ -> },
                fields = listOf(MonarchLocationField.PostalCode),
                errors = mapOf(MonarchLocationField.PostalCode to "Enter a postal code."),
                legend = "Billing address",
            )
        }
        rule.onNode(hasText("Billing address") and isHeading()).assertExists()
        rule.onNodeWithText("Enter a postal code.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theFieldsGrowAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchLocationFields(
                value = MonarchLocationValue(country = "US"),
                onValueChange = { _, _ -> },
                fields = listOf(MonarchLocationField.PostalCode),
                legend = "Shipping address for the warehouse",
            )
        }
        rule.onNodeWithText("Shipping address for the warehouse").assertTextFullyVisible()
    }
}
