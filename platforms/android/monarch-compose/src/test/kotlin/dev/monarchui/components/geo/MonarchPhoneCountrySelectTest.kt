package dev.monarchui.components.geo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.monarchui.components.forms.combobox.ComboboxFilter
import dev.monarchui.components.geo.phonecountryselect.MonarchPhoneCountrySelect
import dev.monarchui.components.geo.phonecountryselect.MonarchPhoneCountrySelectLabels
import dev.monarchui.components.geo.phonecountryselect.PhoneCountrySelectData
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
import kotlin.test.assertTrue

internal object MonarchPhoneCountrySelectFixtures : ComponentFixtures {
    override val id = "phone-country-select"
    override val fixtures = listOf(
        fixture("chosen") {
            MonarchPhoneCountrySelect(selected = "PT", onSelectedChange = { _, _ -> }, locale = Locale.US)
        },
        fixture("empty with helper") {
            MonarchPhoneCountrySelect(
                selected = null,
                onSelectedChange = { _, _ -> },
                locale = Locale.US,
                helperText = "We text a code to this number.",
            )
        },
        fixture("short list with flags and a clear row") {
            MonarchPhoneCountrySelect(
                selected = "GB",
                onSelectedChange = { _, _ -> },
                locale = Locale.US,
                countries = listOf("GB", "IE", "US"),
                showFlag = true,
                clearOptionLabel = "No country",
            )
        },
        fixture("error") {
            MonarchPhoneCountrySelect(
                selected = null,
                onSelectedChange = { _, _ -> },
                locale = Locale.US,
                errorText = "Choose a country code.",
            )
        },
        fixture("disabled") {
            MonarchPhoneCountrySelect(
                selected = "US",
                onSelectedChange = { _, _ -> },
                locale = Locale.US,
                enabled = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchPhoneCountrySelectTest {
    @get:Rule
    val rule = createComposeRule()

    private fun row(text: String) = hasText(text) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)

    private val text: (String, String) -> String = { name, dial -> "$name ($dial)" }

    @Test
    fun theTableParsesCallingCodesAndSharedDigits() {
        val countries = PhoneCountrySelectData.COUNTRIES
        assertEquals(countries.size, countries.map { it.code }.toSet().size)
        assertTrue(countries.all { it.dialCode.matches(Regex("\\+\\d{1,3}")) })
        assertEquals("+351", PhoneCountrySelectData.country("pt")?.dialCode)
        val bahamas = PhoneCountrySelectData.country("BS")
        assertEquals("+1", bahamas?.dialCode)
        assertEquals(listOf("242"), bahamas?.leadingDigits)
        assertEquals(listOf("809", "829", "849"), PhoneCountrySelectData.country("DO")?.leadingDigits)
        assertNull(PhoneCountrySelectData.country("ZZ"))
    }

    @Test
    fun optionsReadNameAndCodeAndMatchTheDigits() {
        val options = PhoneCountrySelectData.options(listOf("PT", "BS", "XX", "PT"), Locale.US, false, text)
        assertEquals(listOf("BS", "PT"), options.map { it.id })
        val portugal = options.last()
        assertEquals("Portugal (+351)", portugal.label)
        assertNull(portugal.hint)
        assertTrue(ComboboxFilter.matches(portugal, "+351"))
        assertTrue(ComboboxFilter.matches(portugal, "351"))
        assertTrue(ComboboxFilter.matches(portugal, "pt"))
        assertTrue(ComboboxFilter.matches(options.first(), "+1242"))
        val flagged = PhoneCountrySelectData.options(listOf("PT"), Locale.US, true, text).single()
        assertEquals("🇵🇹", flagged.hint)
    }

    @Test
    fun choosingReportsTheCodeAndTheCallingCode() {
        var chosen: Pair<String?, String?>? = null
        rule.setMonarchContent {
            var code by remember { mutableStateOf<String?>(null) }
            MonarchPhoneCountrySelect(
                selected = code,
                onSelectedChange = { iso, dial ->
                    code = iso
                    chosen = iso to dial
                },
                locale = Locale.US,
                preferred = listOf("gb"),
            )
        }
        rule.onNodeWithContentDescription("Country code").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Suggested") and isHeading()).assertExists()
        rule.onNode(row("United Kingdom (+44)")).assertExists()
        rule.onNode(hasSetTextAction()).performTextInput("+351")
        rule.waitForIdle()
        rule.onNode(row("Portugal (+351)")).performClick()
        rule.waitForIdle()
        assertEquals("PT" to "+351", chosen)
    }

    @Test
    fun theClearRowReportsNulls() {
        var chosen: Pair<String?, String?>? = null
        rule.setMonarchContent {
            MonarchPhoneCountrySelect(
                selected = "GB",
                onSelectedChange = { iso, dial -> chosen = iso to dial },
                locale = Locale.US,
                countries = listOf("GB", "IE"),
                clearOptionLabel = "No country",
            )
        }
        rule.onNode(hasSetTextAction()).performClick()
        rule.waitForIdle()
        rule.onNode(row("No country")).performClick()
        rule.waitForIdle()
        assertEquals(null to null, chosen)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchPhoneCountrySelect(
                selected = "FR",
                onSelectedChange = { _, _ -> },
                locale = Locale.US,
                countries = listOf("FR", "DE"),
                labels = MonarchPhoneCountrySelectLabels(label = "Indicatif", option = { name, dial -> "$dial $name" }),
            )
        }
        rule.onNodeWithContentDescription("Indicatif").assertExists()
        rule.onNodeWithText("+33 France").assertExists()
    }

    @Test
    fun disabledIsNotEnabledAndTextFitsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchPhoneCountrySelect(
                selected = "US",
                onSelectedChange = { _, _ -> },
                locale = Locale.US,
                enabled = false,
            )
        }
        rule.onNodeWithText("United States (+1)").assertIsNotEnabled()
        rule.onNodeWithText("Country code", useUnmergedTree = true).assertTextFullyVisible()
    }
}
