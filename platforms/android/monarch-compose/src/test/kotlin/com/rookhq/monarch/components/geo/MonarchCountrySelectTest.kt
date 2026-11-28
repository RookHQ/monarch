package com.rookhq.monarch.components.geo

import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxOption
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxPresentation
import com.rookhq.monarch.components.geo.countryselect.CountrySelectData
import com.rookhq.monarch.components.geo.countryselect.CountrySelectStyle
import com.rookhq.monarch.components.geo.countryselect.MonarchCountrySelect
import com.rookhq.monarch.components.geo.countryselect.MonarchCountrySelectLabels
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchCountrySelectFixtures : ComponentFixtures {
    override val id = "country-select"
    override val fixtures = listOf(
        fixture("chosen, with code") {
            MonarchCountrySelect(selected = "PT", onSelectedChange = {}, locale = Locale.US)
        },
        fixture("empty, helper and clear icon") {
            MonarchCountrySelect(
                selected = null,
                onSelectedChange = {},
                locale = Locale.US,
                helperText = "Where the order ships.",
                clearIcon = { Icon(TestIcon, contentDescription = null) },
            )
        },
        fixture("chosen, clear icon") {
            MonarchCountrySelect(
                selected = "DE",
                onSelectedChange = {},
                locale = Locale.US,
                clearIcon = { Icon(TestIcon, contentDescription = null) },
            )
        },
        fixture("error") {
            MonarchCountrySelect(selected = null, onSelectedChange = {}, locale = Locale.US, errorText = "Choose one.")
        },
        fixture("short list in a menu, flags") {
            MonarchCountrySelect(
                selected = "GB",
                onSelectedChange = {},
                locale = Locale.US,
                countries = listOf("GB", "IE", "FR"),
                showFlag = true,
            )
        },
        fixture("long name") {
            MonarchCountrySelect(selected = "GS", onSelectedChange = {}, locale = Locale.US)
        },
        fixture("disabled") {
            MonarchCountrySelect(selected = "JP", onSelectedChange = {}, locale = Locale.US, enabled = false)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchCountrySelectTest {
    @get:Rule
    val rule = createComposeRule()

    private val plain = CountrySelectStyle(showFlag = false, showCode = true, countryName = null)

    /** A row of the list: it has a selected state, unlike the field that may show the same text. */
    private fun row(text: String) = hasText(text) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)

    @Test
    fun theBundledListHasEveryAssignedCodeOnce() {
        assertEquals(249, CountrySelectData.CODES.size)
        assertEquals(249, CountrySelectData.CODES.toSet().size)
        assertTrue(CountrySelectData.CODES.all { it.matches(Regex("[A-Z]{2}")) })
        val codes = CountrySelectData.codes(listOf(" pt", "de", "PT", "fr"), listOf("FR"))
        assertEquals(listOf("PT", "DE"), codes)
    }

    @Test
    fun namesComeFromTheLocaleAndSortByItsCollation() {
        assertEquals("Germany", CountrySelectData.name("DE", Locale.US))
        assertEquals("Allemagne", CountrySelectData.name("DE", Locale.FRANCE))
        val options = CountrySelectData.options(CountrySelectData.CODES, null, Locale.US, plain)
        assertEquals(listOf("AF", "AX", "AL"), options.take(3).map { it.id })
        assertEquals("PT", options.first { it.label == "Portugal" }.hint)
        assertNull(options.first().keywords)
        val french = CountrySelectData.options(listOf("DE"), null, Locale.FRANCE, plain).single()
        assertEquals("Germany", french.keywords)
    }

    @Test
    fun flagsAreRegionalIndicatorsAndOptionalInTheHint() {
        assertEquals("🇵🇹", CountrySelectData.flag("pt"))
        assertEquals("", CountrySelectData.flag("P1"))
        assertEquals("", CountrySelectData.flag("PRT"))
        val flagged = CountrySelectData.option("PT", Locale.US, CountrySelectStyle(true, true, null))
        assertEquals("🇵🇹 PT", flagged.hint)
        assertNull(CountrySelectData.option("PT", Locale.US, CountrySelectStyle(false, false, null)).hint)
        val named = CountrySelectData.option("TW", Locale.US, CountrySelectStyle(false, true) { "Taiwan" })
        assertEquals("Taiwan", named.label)
    }

    @Test
    fun aSelectedCodeOutsideTheListIsStillListed() {
        val options = CountrySelectData.options(listOf("FR"), "XK", Locale.US, plain)
        assertEquals(setOf("FR", "XK"), options.map { it.id }.toSet())
    }

    @Test
    fun everyTypedWordMustMatchTheNameCodeOrKeywords() {
        val gb = CountrySelectData.option("GB", Locale.US, plain)
        assertTrue(CountrySelectData.matches(gb, "united king"))
        assertTrue(CountrySelectData.matches(gb, "gb"))
        assertFalse(CountrySelectData.matches(gb, "united states"))
        val french = CountrySelectData.option("DE", Locale.FRANCE, plain)
        assertTrue(CountrySelectData.matches(french, "germ"))
        assertTrue(CountrySelectData.matches(CountrySelectData.option("AX", Locale.US, plain), "aland"))
    }

    @Test
    fun priorityCountriesComeFirstOnceAndOnlyWhileNotSearching() {
        val options = listOf("FR", "PT", "US").map { MonarchComboboxOption(it, it) }
        val arranged = CountrySelectData.arrange(options, listOf("US", "XX", "US"), false, "Top", "Rest")
        assertEquals(listOf("US", "FR", "PT"), arranged.map { it.id })
        assertEquals(listOf("Top", "Rest", "Rest"), arranged.map { it.group })
        assertEquals(options, CountrySelectData.arrange(options, listOf("US"), true, "Top", "Rest"))
        assertEquals(options, CountrySelectData.arrange(options, emptyList(), false, "Top", "Rest"))
    }

    @Test
    fun searchingTheSheetChoosesAnUpperCaseCode() {
        var chosen: String? = "unset"
        rule.setMonarchContent {
            var code by remember { mutableStateOf<String?>("de") }
            MonarchCountrySelect(
                selected = code,
                onSelectedChange = {
                    code = it
                    chosen = it
                },
                locale = Locale.US,
                priority = listOf("US"),
            )
        }
        rule.onNodeWithText("Germany").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Country") and isHeading()).assertExists()
        rule.onNode(hasText("Suggested") and isHeading()).assertExists()
        rule.onNode(row("United States")).assertExists()
        rule.onNode(hasSetTextAction()).performTextInput("portu")
        rule.waitForIdle()
        rule.onNode(hasText("Suggested") and isHeading()).assertDoesNotExist()
        rule.onNode(row("United States")).assertDoesNotExist()
        rule.onNode(row("Portugal")).performClick()
        rule.waitForIdle()
        assertEquals("PT", chosen)
        rule.onNodeWithText("Portugal").assertExists()
    }

    @Test
    fun theChosenCountryIsSelectedInTheMenu() {
        rule.setMonarchContent {
            MonarchCountrySelect(
                selected = "ie",
                onSelectedChange = {},
                locale = Locale.US,
                countries = listOf("GB", "IE", "FR"),
                presentation = MonarchComboboxPresentation.Menu,
            )
        }
        rule.onNode(hasSetTextAction()).performClick()
        rule.waitForIdle()
        rule.onNode(row("Ireland")).assertIsSelected()
    }

    @Test
    fun nothingMatchingSaysSo() {
        rule.setMonarchContent {
            MonarchCountrySelect(selected = null, onSelectedChange = {}, locale = Locale.US)
        }
        rule.onNodeWithContentDescription("Country").performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextInput("qqqq")
        rule.waitForIdle()
        rule.onNodeWithText("No countries match").assertExists()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchCountrySelect(
                selected = null,
                onSelectedChange = {},
                locale = Locale.US,
                priority = listOf("FR"),
                labels = MonarchCountrySelectLabels(label = "Pays", suggested = "Proposés", empty = "Aucun"),
            )
        }
        rule.onNodeWithContentDescription("Pays").performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Proposés") and isHeading()).assertExists()
        rule.onNode(hasSetTextAction()).performTextInput("qqqq")
        rule.waitForIdle()
        rule.onNodeWithText("Aucun").assertExists()
    }

    @Test
    fun disabledDoesNotOpen() {
        rule.setMonarchContent {
            MonarchCountrySelect(selected = "JP", onSelectedChange = {}, locale = Locale.US, enabled = false)
        }
        rule.onNodeWithText("Japan").assertIsNotEnabled().performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Country") and isHeading()).assertDoesNotExist()
    }

    @Test
    fun theErrorIsTheFieldsErrorAndTheNameFitsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale, layoutDirection = LayoutDirection.Rtl) {
            MonarchCountrySelect(
                selected = "GS",
                onSelectedChange = {},
                locale = Locale.US,
                errorText = "Choose where you live.",
            )
        }
        rule.onNodeWithText("Choose where you live.", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNode(hasText("South Georgia", substring = true))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Choose where you live."))
    }
}
