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
import dev.monarchui.components.geo.countryselect.CountrySelectData
import dev.monarchui.components.geo.currencyselect.CurrencySelectData
import dev.monarchui.components.geo.currencyselect.MonarchCurrencySelect
import dev.monarchui.components.geo.currencyselect.MonarchCurrencySelectDisplay
import dev.monarchui.components.geo.currencyselect.MonarchCurrencySelectLabels
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

internal object MonarchCurrencySelectFixtures : ComponentFixtures {
    override val id = "currency-select"
    override val fixtures = listOf(
        fixture("name") {
            MonarchCurrencySelect(selected = "EUR", onSelectedChange = {}, locale = Locale.US)
        },
        fixture("code") {
            MonarchCurrencySelect(
                selected = "EUR",
                onSelectedChange = {},
                locale = Locale.US,
                display = MonarchCurrencySelectDisplay.Code,
            )
        },
        fixture("code and name") {
            MonarchCurrencySelect(
                selected = "USD",
                onSelectedChange = {},
                locale = Locale.US,
                display = MonarchCurrencySelectDisplay.CodeAndName,
            )
        },
        fixture("empty with helper, short list") {
            MonarchCurrencySelect(
                selected = null,
                onSelectedChange = {},
                locale = Locale.US,
                currencies = listOf("USD", "EUR", "GBP"),
                helperText = "Invoices use this currency.",
            )
        },
        fixture("error") {
            MonarchCurrencySelect(selected = null, onSelectedChange = {}, locale = Locale.US, errorText = "Choose one.")
        },
        fixture("disabled") {
            MonarchCurrencySelect(selected = "JPY", onSelectedChange = {}, locale = Locale.US, enabled = false)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchCurrencySelectTest {
    @get:Rule
    val rule = createComposeRule()

    private fun row(text: String) = hasText(text) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)

    @Test
    fun theBundledListHasActiveCodesOnly() {
        val codes = CurrencySelectData.CODES
        assertEquals(codes.size, codes.toSet().size)
        assertTrue("EUR" in codes && "USD" in codes)
        assertTrue("HRK" !in codes && "XAU" !in codes && "XTS" !in codes)
    }

    @Test
    fun namesSymbolsAndLabelsFollowTheLocaleAndDisplay() {
        assertEquals("Euro", CurrencySelectData.name("EUR", Locale.US))
        assertEquals("$", CurrencySelectData.symbol("USD", Locale.US))
        assertEquals("QQQ", CurrencySelectData.name("QQQ", Locale.US))
        assertEquals("QQQ", CurrencySelectData.symbol("QQQ", Locale.US))
        assertEquals("Euro", CurrencySelectData.label("EUR", "Euro", MonarchCurrencySelectDisplay.Name))
        assertEquals("EUR", CurrencySelectData.label("EUR", "Euro", MonarchCurrencySelectDisplay.Code))
        assertEquals("EUR · Euro", CurrencySelectData.label("EUR", "Euro", MonarchCurrencySelectDisplay.CodeAndName))
    }

    @Test
    fun optionsShowTheCodeAndSymbolAndFindTheSymbol() {
        val usd = CurrencySelectData.option("USD", Locale.US, MonarchCurrencySelectDisplay.Name, showSymbol = true)
        assertEquals("US Dollar", usd.label)
        assertNull(usd.description)
        assertEquals("USD $", usd.hint)
        assertTrue(CountrySelectData.matches(usd, "$"))
        val code = CurrencySelectData.option("USD", Locale.US, MonarchCurrencySelectDisplay.Code, showSymbol = false)
        assertEquals("USD", code.label)
        assertEquals("US Dollar", code.description)
        assertEquals("USD", code.hint)
        val german = CurrencySelectData.option("USD", Locale.GERMANY, MonarchCurrencySelectDisplay.Name, true)
        assertTrue(CountrySelectData.matches(german, "dollar"))
    }

    @Test
    fun optionsSortByNameAndKeepAnUnlistedSelection() {
        val options = CurrencySelectData.options(
            listOf("usd", "EUR", "GBP"),
            "CHF",
            Locale.US,
            MonarchCurrencySelectDisplay.Name,
            showSymbol = true,
        )
        assertEquals(listOf("GBP", "EUR", "CHF", "USD"), options.map { it.id })
    }

    @Test
    fun searchingChoosesACurrency() {
        var chosen: String? = null
        rule.setMonarchContent {
            var code by remember { mutableStateOf<String?>(null) }
            MonarchCurrencySelect(
                selected = code,
                onSelectedChange = {
                    code = it
                    chosen = it
                },
                locale = Locale.US,
                priority = listOf("EUR"),
            )
        }
        rule.onNodeWithContentDescription("Currency").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Suggested") and isHeading()).assertExists()
        rule.onNode(hasSetTextAction()).performTextInput("yen")
        rule.waitForIdle()
        rule.onNode(row("Japanese Yen")).performClick()
        rule.waitForIdle()
        assertEquals("JPY", chosen)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchCurrencySelect(
                selected = null,
                onSelectedChange = {},
                locale = Locale.US,
                labels = MonarchCurrencySelectLabels(label = "Devise", empty = "Aucune"),
            )
        }
        rule.onNodeWithContentDescription("Devise").performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextInput("qqqq")
        rule.waitForIdle()
        rule.onNodeWithText("Aucune").assertExists()
    }

    @Test
    fun disabledIsNotEnabledAndTextFitsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchCurrencySelect(
                selected = "XAF",
                onSelectedChange = {},
                locale = Locale.US,
                display = MonarchCurrencySelectDisplay.CodeAndName,
                enabled = false,
            )
        }
        rule.onNode(hasText("XAF", substring = true)).assertIsNotEnabled()
        rule.onNodeWithText("Currency", useUnmergedTree = true).assertTextFullyVisible()
    }
}
