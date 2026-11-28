package com.rookhq.monarch.components.billing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.billing.money.MonarchMoney
import com.rookhq.monarch.components.billing.money.MonarchMoneyCurrencyDisplay
import com.rookhq.monarch.components.billing.money.MonarchMoneyFormat
import com.rookhq.monarch.components.billing.money.MonarchMoneyNotation
import com.rookhq.monarch.components.billing.money.MonarchMoneySignDisplay
import com.rookhq.monarch.components.billing.money.MonarchMoneyTokens
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object MonarchMoneyFixtures : ComponentFixtures {
    override val id = "money"
    override val fixtures = listOf(
        fixture("standard, negative and toned") {
            Column {
                MonarchMoney(amount = BigDecimal("1234.5"), currencyCode = "USD")
                MonarchMoney(minorUnits = -4200L, currencyCode = "EUR", toneNegative = true)
                MonarchMoney(minorUnits = -4200L, currencyCode = "USD", accountingSign = true)
            }
        },
        fixture("compact and signs") {
            Column {
                MonarchMoney(BigDecimal("1300000"), "USD", notation = MonarchMoneyNotation.Compact)
                MonarchMoney(BigDecimal("42"), "USD", signDisplay = MonarchMoneySignDisplay.Always)
                MonarchMoney(BigDecimal("-42"), "USD", signDisplay = MonarchMoneySignDisplay.Never)
            }
        },
        fixture("every currency display") {
            Column {
                MonarchMoneyCurrencyDisplay.entries.forEach {
                    MonarchMoney(amount = BigDecimal("19.99"), currencyCode = "USD", currencyDisplay = it)
                }
            }
        },
        fixture("type roles") {
            Column {
                MonarchMoney(amount = BigDecimal("99"), currencyCode = "JPY", style = MonarchTextStyle.Display)
                MonarchMoney(amount = BigDecimal("1.234"), currencyCode = "KWD", style = MonarchTextStyle.Caption)
            }
        },
        fixture("empty") { MonarchMoney(amount = null, currencyCode = "USD") },
        fixture("long amount") {
            MonarchMoney(
                amount = BigDecimal("123456789012.34"),
                currencyCode = "USD",
                currencyDisplay = MonarchMoneyCurrencyDisplay.Name,
                style = MonarchTextStyle.Title,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchMoneyTest {
    @get:Rule
    val rule = createComposeRule()

    private val us = Locale.US

    private fun format(
        amount: String,
        code: String = "USD",
        notation: MonarchMoneyNotation = MonarchMoneyNotation.Standard,
        sign: MonarchMoneySignDisplay = MonarchMoneySignDisplay.Automatic,
        display: MonarchMoneyCurrencyDisplay = MonarchMoneyCurrencyDisplay.Symbol,
        accounting: Boolean = false,
        digits: IntRange? = null,
    ) = MonarchMoneyFormat.format(BigDecimal(amount), code, us, notation, sign, display, accounting, digits)

    @Test
    fun theCurrencyDecidesTheSymbolAndTheDigits() {
        assertEquals("$1,234.50", format("1234.5"))
        assertEquals("¥1,235", format("1234.5", "JPY"))
        assertEquals("KWD 1.234", format("1.2344", "KWD").replace(' ', ' '))
        assertEquals("$1,234", format("1234.4", digits = 0..0))
        assertEquals("$1,234.568", format("1234.5678", digits = 2..3))
    }

    @Test
    fun theSignFollowsTheSignDisplay() {
        assertEquals("-$42.00", format("-42"))
        assertEquals("+$42.00", format("42", sign = MonarchMoneySignDisplay.Always))
        assertEquals("+$0.00", format("0", sign = MonarchMoneySignDisplay.Always))
        assertEquals("+$42.00", format("42", sign = MonarchMoneySignDisplay.ExceptZero))
        assertEquals("$0.00", format("0", sign = MonarchMoneySignDisplay.ExceptZero))
        assertEquals("-$42.00", format("-42", sign = MonarchMoneySignDisplay.ExceptZero))
        assertEquals("$42.00", format("-42", sign = MonarchMoneySignDisplay.Never))
        assertEquals("($42.00)", format("-42", accounting = true))
        assertEquals("$42.00", format("42", accounting = true))
    }

    @Test
    fun theCurrencyCanBeWrittenAsCodeOrName() {
        assertTrue(format("19.99", display = MonarchMoneyCurrencyDisplay.Code).startsWith("USD"))
        assertEquals("19.99 US dollars", format("19.99", display = MonarchMoneyCurrencyDisplay.Name))
    }

    @Test
    fun theCompactNotationKeepsTheSymbolInPlace() {
        assertEquals("$1.3M", format("1300000", notation = MonarchMoneyNotation.Compact))
        assertEquals("-$45K", format("-45000", notation = MonarchMoneyNotation.Compact))
        assertEquals("($45K)", format("-45000", notation = MonarchMoneyNotation.Compact, accounting = true))
        val always = MonarchMoneySignDisplay.Always
        assertEquals("+$45K", format("45000", notation = MonarchMoneyNotation.Compact, sign = always))
    }

    @Test
    fun minorUnitsAreConvertedExactly() {
        assertEquals(BigDecimal("19.99"), MonarchMoneyFormat.amountOfMinorUnits(1999, "USD"))
        assertEquals(BigDecimal("500"), MonarchMoneyFormat.amountOfMinorUnits(500, "JPY"))
        assertEquals(BigDecimal("1.234"), MonarchMoneyFormat.amountOfMinorUnits(1234, "KWD"))
        assertEquals(BigDecimal("92233720368547758.07"), MonarchMoneyFormat.amountOfMinorUnits(Long.MAX_VALUE, "USD"))
        assertEquals(2, MonarchMoneyFormat.currencyDigits("not a code"))
    }

    @Test
    fun anUnknownCodeIsWrittenAfterTheNumber() {
        assertEquals("12.5 rupees", MonarchMoneyFormat.format(BigDecimal("12.5"), "rupees", us))
    }

    @Test
    fun theAmountIsShownInTheLocaleOfTheConfigurationUnlessOneIsGiven() {
        rule.setMonarchContent {
            Column {
                MonarchMoney(minorUnits = 1999L, currencyCode = "USD")
                MonarchMoney(amount = BigDecimal("1234.5"), currencyCode = "EUR", locale = Locale.GERMANY)
            }
        }
        rule.onNodeWithText("$19.99").assertExists()
        rule.onNodeWithText("1.234,50 €").assertExists()
    }

    @Test
    fun aCompactAmountIsReadInFull() {
        rule.setMonarchContent {
            MonarchMoney(amount = BigDecimal("1300000"), currencyCode = "USD", notation = MonarchMoneyNotation.Compact)
        }
        rule.onNodeWithText("$1.3M").assertExists()
        rule.onNodeWithContentDescription("$1,300,000.00").assertExists()
    }

    @Test
    fun aStandardAmountHasNoSeparateSpokenForm() {
        rule.setMonarchContent { MonarchMoney(amount = BigDecimal("5"), currencyCode = "USD") }
        rule.onNodeWithText("$5.00").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }

    @Test
    fun aMissingAmountIsADashReadAsNoAmountAndBothCanBeReplaced() {
        rule.setMonarchContent {
            Column {
                MonarchMoney(amount = null, currencyCode = "USD")
                MonarchMoney(minorUnits = null, currencyCode = "USD", emptyText = "n/a", emptyLabel = "Not billed")
            }
        }
        rule.onNodeWithText("—").assertExists()
        rule.onNodeWithContentDescription("No amount").assertExists()
        rule.onNodeWithText("n/a").assertExists()
        rule.onNodeWithContentDescription("Not billed").assertExists()
    }

    @Test
    fun aLongAmountWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchMoney(
                amount = BigDecimal("123456789.5"),
                currencyCode = "USD",
                modifier = Modifier.width(200.dp),
                currencyDisplay = MonarchMoneyCurrencyDisplay.Name,
                locale = Locale.US,
            )
        }
        rule.onNodeWithText("US dollars", substring = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val t = MonarchMoneyTokens
            assertEquals(colors.dangerStrong, t.textColor(true, false, Color.Unspecified, colors), theme.id)
            assertEquals(colors.textMuted, t.textColor(false, true, Color.Unspecified, colors), theme.id)
            assertEquals(Color.Unspecified, t.textColor(false, false, Color.Unspecified, colors), theme.id)
            assertEquals(colors.textLink, t.textColor(false, false, colors.textLink, colors), theme.id)
        }
    }
}
