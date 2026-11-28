@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.billing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.billing.lineitemseditor.MonarchLineItem
import dev.monarchui.components.billing.lineitemseditor.MonarchLineItemsDiscount
import dev.monarchui.components.billing.lineitemseditor.MonarchLineItemsDiscountKind
import dev.monarchui.components.billing.lineitemseditor.MonarchLineItemsEditor
import dev.monarchui.components.billing.lineitemseditor.MonarchLineItemsEditorLabels
import dev.monarchui.components.billing.lineitemseditor.MonarchLineItemsEditorTokens
import dev.monarchui.components.billing.lineitemseditor.MonarchLineItemsTaxRateOption
import dev.monarchui.components.billing.lineitemseditor.MonarchLineItemsTotals
import dev.monarchui.components.billing.money.MonarchMoneyFormat
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val workshop = MonarchLineItem("r1", "Discovery workshop", BigDecimal.ONE, BigDecimal(800))
private val hours = MonarchLineItem("r2", "Design hours", BigDecimal(12), BigDecimal(95))
private val fivePercent = MonarchLineItemsDiscount(MonarchLineItemsDiscountKind.Percent, BigDecimal(5))

internal object MonarchLineItemsEditorFixtures : ComponentFixtures {
    override val id = "line-items-editor"
    override val fixtures = listOf(
        fixture("one row, required, no totals") {
            MonarchLineItemsEditor(
                items = listOf(hours),
                onItemsChange = {},
                currencyCode = "USD",
                required = true,
                showTotals = false,
            )
        },
        fixture("at the minimum, with an error") {
            MonarchLineItemsEditor(
                items = listOf(MonarchLineItem("x", "")),
                onItemsChange = {},
                currencyCode = "JPY",
                minimumItems = 1,
                error = "Add a price to every item",
                showTotals = false,
            )
        },
        fixture("empty with totals") {
            MonarchLineItemsEditor(
                items = emptyList(),
                onItemsChange = {},
                currencyCode = "EUR",
                taxRate = BigDecimal(19),
            )
        },
        fixture("read-only with discount and tax") {
            MonarchLineItemsEditor(
                items = listOf(workshop, hours),
                onItemsChange = {},
                currencyCode = "USD",
                taxRate = BigDecimal(18),
                discount = fivePercent,
                readOnly = true,
            )
        },
        fixture("disabled") {
            MonarchLineItemsEditor(
                items = listOf(hours),
                onItemsChange = {},
                currencyCode = "USD",
                enabled = false,
                showTotals = false,
            )
        },
        fixture("totals with editable discount and tax menu") {
            MonarchLineItemsEditor(
                items = emptyList(),
                onItemsChange = {},
                currencyCode = "USD",
                taxRate = BigDecimal(18),
                onTaxRateChange = {},
                taxRateOptions = listOf(
                    MonarchLineItemsTaxRateOption(BigDecimal.ZERO, "No tax"),
                    MonarchLineItemsTaxRateOption(BigDecimal(18), "GST 18%"),
                ),
                discount = fivePercent,
                onDiscountChange = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchLineItemsEditorTest {
    @get:Rule
    val rule = createComposeRule()

    private fun money(major: String, code: String = "USD"): String =
        MonarchMoneyFormat.format(BigDecimal(major), code, Locale.US)

    @Test
    fun totalsAreWorkedOutExactlyInMinorUnits() {
        val totals = MonarchLineItemsTotals(listOf(workshop, hours), "USD", BigDecimal(18), fivePercent)
        assertEquals(listOf(80_000L, 114_000L), totals.lines)
        assertEquals(194_000L, totals.subtotal)
        assertEquals(9_700L, totals.discount)
        assertEquals(33_174L, totals.tax)
        assertEquals(217_474L, totals.total)
        fun one(quantity: Int, price: String) =
            listOf(MonarchLineItem("a", "", BigDecimal(quantity), BigDecimal(price)))
        assertEquals(listOf(101L), MonarchLineItemsTotals(one(1, "1.005"), "USD").lines)
        assertEquals(299L, MonarchLineItemsTotals(one(3, "99.5"), "JPY").total)
        assertEquals(1_235L, MonarchLineItemsTotals(one(1, "1.2345"), "KWD").total)
        val tenths = List(10) { MonarchLineItem("t$it", "", BigDecimal.ONE, BigDecimal("0.10")) }
        assertEquals(100L, MonarchLineItemsTotals(tenths, "USD").total, "ten times 0.10 is exactly 1.00")
    }

    @Test
    fun discountsAreCappedAndEmptyFieldsCountAsZero() {
        val one = listOf(MonarchLineItem("a", "", BigDecimal(2), BigDecimal(50)))
        val big = MonarchLineItemsDiscount(MonarchLineItemsDiscountKind.Amount, BigDecimal(500))
        assertEquals(10_000L, MonarchLineItemsTotals(one, "USD", discount = big).discount)
        val over = MonarchLineItemsDiscount(MonarchLineItemsDiscountKind.Percent, BigDecimal(150))
        assertEquals(0L, MonarchLineItemsTotals(one, "USD", discount = over).total)
        val negative = MonarchLineItemsDiscount(MonarchLineItemsDiscountKind.Amount, BigDecimal(-5))
        assertEquals(0L, MonarchLineItemsTotals(one, "USD", discount = negative).discount)
        val credit = listOf(MonarchLineItem("c", "", BigDecimal(-1), BigDecimal(50)))
        assertEquals(0L, MonarchLineItemsTotals(credit, "USD", discount = big).discount)
        val empty = listOf(MonarchLineItem("e", "", null, null), MonarchLineItem("f", "", BigDecimal(3), null))
        assertEquals(listOf(0L, 0L), MonarchLineItemsTotals(empty, "USD").lines)
    }

    @Test
    fun theHeadingIsReadWithRequiredAndEachRowWithItsAmount() {
        rule.setMonarchContent {
            MonarchLineItemsEditor(
                items = listOf(workshop, hours),
                onItemsChange = {},
                currencyCode = "USD",
                required = true,
            )
        }
        rule.onNodeWithContentDescription("Line items, Required").assert(isHeading())
        rule.onNodeWithContentDescription("Item 2, Amount ${money("1140.00")}").assert(isHeading())
    }

    @Test
    fun theTotalsAreReadAsTitleAndAmount() {
        rule.setMonarchContent {
            MonarchLineItemsEditor(
                items = listOf(workshop, hours),
                onItemsChange = {},
                currencyCode = "USD",
                taxRate = BigDecimal(18),
                discount = fivePercent,
                readOnly = true,
            )
        }
        rule.onNodeWithContentDescription("Subtotal, ${money("1940.00")}").assertExists()
        rule.onNodeWithContentDescription("Discount (5%), ${money("-97.00")}").assertExists()
        rule.onNodeWithContentDescription("Tax (18%), ${money("331.74")}").assertExists()
        rule.onNodeWithContentDescription("Total, ${money("2174.74")}").assertExists()
    }

    @Test
    fun removingARowCallsBackAndIsAnnounced() {
        var items by mutableStateOf(listOf(workshop, hours))
        rule.setMonarchContent {
            MonarchLineItemsEditor(items = items, onItemsChange = { items = it }, currencyCode = "USD")
        }
        rule.onNodeWithContentDescription("Remove item 2: Design hours")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitForIdle()
        assertEquals(listOf("r1"), items.map { it.id })
        rule.onNodeWithContentDescription("Removed item 2")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun addingARowUsesMakeItemAndIsAnnounced() {
        var items by mutableStateOf(emptyList<MonarchLineItem>())
        rule.setMonarchContent {
            MonarchLineItemsEditor(
                items = items,
                onItemsChange = { items = it },
                currencyCode = "USD",
                showTotals = false,
                makeItem = { MonarchLineItem("new", "Travel") },
            )
        }
        rule.onNode(hasText("Add item") and hasClickAction()).assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(listOf("new"), items.map { it.id })
        rule.onNodeWithContentDescription("Added item 1").assertExists()
        rule.onNodeWithContentDescription("Remove item 1: Travel").assertExists()
    }

    @Test
    fun atTheMinimumRowsCannotBeRemoved() {
        rule.setMonarchContent {
            MonarchLineItemsEditor(items = listOf(hours), onItemsChange = {}, currencyCode = "USD", minimumItems = 1)
        }
        rule.onNodeWithContentDescription("Remove item 1: Design hours").assertIsNotEnabled()
        rule.onNode(hasText("Add item") and hasClickAction()).assertIsEnabled()
    }

    @Test
    fun aTypedQuantityAndPriceUpdateTheRow() {
        var items by mutableStateOf(listOf(MonarchLineItem("a", "Hosting", BigDecimal.ONE, null)))
        rule.setMonarchContent {
            MonarchLineItemsEditor(items = items, onItemsChange = { items = it }, currencyCode = "USD")
        }
        val quantity = rule.onNode(hasSetTextAction() and hasContentDescription("Quantity"))
        quantity.performTextClearance()
        quantity.performTextInput("3")
        quantity.performImeAction()
        rule.waitForIdle()
        assertEquals(0, BigDecimal(3).compareTo(items.single().quantity))
        val price = rule.onNode(hasSetTextAction() and hasContentDescription("Unit price (USD)"))
        price.performTextInput("24.5")
        price.performImeAction()
        rule.waitForIdle()
        assertEquals(0, BigDecimal("24.50").compareTo(items.single().unitPrice))
        rule.onNodeWithContentDescription("Item 1, Amount ${money("73.50")}").assertExists()
    }

    @Test
    fun aDescriptionEditKeepsTheRowsId() {
        var items by mutableStateOf(listOf(workshop))
        rule.setMonarchContent {
            MonarchLineItemsEditor(items = items, onItemsChange = { items = it }, currencyCode = "USD")
        }
        rule.onNode(hasSetTextAction() and hasText("Discovery workshop")).performTextInput(" day")
        rule.waitForIdle()
        assertEquals("r1", items.single().id)
        assertTrue(items.single().description.contains("day"), items.single().description)
    }

    @Test
    fun readOnlyRowsAreTextWithoutControls() {
        rule.setMonarchContent {
            MonarchLineItemsEditor(items = listOf(hours), onItemsChange = {}, currencyCode = "USD", readOnly = true)
        }
        rule.onNodeWithText("12 × ${money("95.00")}", substring = false).assertExists()
        rule.onNodeWithText("Add item").assertDoesNotExist()
        rule.onNodeWithText("Remove").assertDoesNotExist()
        rule.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test
    fun disabledLocksEveryControlAndHidesTheRateEditors() {
        rule.setMonarchContent {
            MonarchLineItemsEditor(
                items = listOf(hours),
                onItemsChange = {},
                currencyCode = "USD",
                taxRate = BigDecimal(18),
                onTaxRateChange = {},
                enabled = false,
            )
        }
        rule.onNode(hasText("Add item") and hasClickAction()).assertIsNotEnabled()
        rule.onNodeWithContentDescription("Remove item 1: Design hours").assertIsNotEnabled()
        rule.onNode(hasSetTextAction() and hasContentDescription("Tax rate")).assertDoesNotExist()
    }

    @Test
    fun anEditableDiscountChangesItsKindAndTheTaxRateComesFromTheMenu() {
        var discount by mutableStateOf(fivePercent)
        val rates = mutableListOf<BigDecimal>()
        rule.setMonarchContent {
            MonarchLineItemsEditor(
                items = emptyList(),
                onItemsChange = {},
                currencyCode = "USD",
                taxRate = BigDecimal(18),
                onTaxRateChange = { rates += it },
                taxRateOptions = listOf(
                    MonarchLineItemsTaxRateOption(BigDecimal.ZERO, "No tax"),
                    MonarchLineItemsTaxRateOption(BigDecimal(18), "GST 18%"),
                ),
                discount = discount,
                onDiscountChange = { discount = it },
            )
        }
        rule.onNodeWithContentDescription("Discount type").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("USD").performClick()
        rule.waitForIdle()
        assertEquals(MonarchLineItemsDiscountKind.Amount, discount.kind)
        rule.onNodeWithText("GST 18%").assertExists()
    }

    @Test
    fun anErrorIsShownAndAnnounced() {
        rule.setMonarchContent {
            MonarchLineItemsEditor(
                items = emptyList(),
                onItemsChange = {},
                currencyCode = "USD",
                error = "Add at least one item",
            )
        }
        rule.onNodeWithText("No items yet. Add one to start.").assertExists()
        rule.onNodeWithText("Add at least one item")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun rightToLeftPutsTheRemoveButtonAtTheStart() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchLineItemsEditor(items = listOf(hours), onItemsChange = {}, currencyCode = "USD", showTotals = false)
        }
        val header = rule.onNodeWithContentDescription("Item 1, Amount ${money("1140.00")}")
        header.assertIsBefore(rule.onNodeWithContentDescription("Remove item 1: Design hours"), LayoutDirection.Rtl)
    }

    @Test
    fun theFieldsStackAndWrapAtTheLargestFontSize() {
        assertTrue(MonarchLineItemsEditorTokens.stacks(LargestFontScale))
        assertTrue(!MonarchLineItemsEditorTokens.stacks(1f))
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchLineItemsEditor(items = emptyList(), onItemsChange = {}, currencyCode = "USD")
        }
        rule.onNodeWithText("No items yet. Add one to start.").assertTextFullyVisible()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchLineItemsEditor(
                items = listOf(hours),
                onItemsChange = {},
                currencyCode = "EUR",
                labels = MonarchLineItemsEditorLabels(
                    label = "Positionen",
                    item = { "Position $it" },
                    amount = "Betrag",
                    quantity = "Menge",
                    unitPrice = { "Einzelpreis ($it)" },
                    addItem = "Position hinzufügen",
                    removeItem = { number, _ -> "Position $number entfernen" },
                    total = "Gesamt",
                ),
            )
        }
        rule.onNodeWithContentDescription("Positionen").assert(isHeading())
        rule.onNodeWithContentDescription("Position 1, Betrag ${money("1140.00", "EUR")}").assertExists()
        rule.onNode(hasSetTextAction() and hasContentDescription("Menge")).assertExists()
        rule.onNode(hasSetTextAction() and hasContentDescription("Einzelpreis (EUR)")).assertExists()
        rule.onNodeWithText("Position hinzufügen").assertExists()
        rule.onNodeWithContentDescription("Position 1 entfernen").assertExists()
        rule.onNodeWithContentDescription("Gesamt, ${money("1140.00", "EUR")}").assertExists()
    }

    @Test
    fun helpersConvertExactly() {
        assertEquals(1999L, MonarchLineItemsEditorTokens.toMinor(BigDecimal("19.99"), 2))
        assertEquals(0, BigDecimal("19.99").compareTo(MonarchLineItemsEditorTokens.fromMinor(1999L, 2)))
        assertEquals(0, BigDecimal("1.5").compareTo(MonarchLineItemsEditorTokens.decimal(1.5)))
        assertEquals("18%", MonarchLineItemsEditorTokens.percent(BigDecimal(18), Locale.US))
        assertEquals("7.125%", MonarchLineItemsEditorTokens.percent(BigDecimal("7.125"), Locale.US))
        assertEquals("1.5", MonarchLineItemsEditorTokens.quantity(BigDecimal("1.50"), Locale.US))
        assertTrue(MonarchLineItemsEditorTokens.removable(2, 1))
        assertTrue(!MonarchLineItemsEditorTokens.removable(1, 1))
    }

    @Test
    fun coloursComeFromTheThemeInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val parts = MonarchLineItemsEditorTokens.colors(colors)
            assertEquals(colors.textDefault, parts.heading, theme.id)
            assertEquals(colors.dangerStrong, parts.required, theme.id)
            assertEquals(colors.dangerStrong, parts.error, theme.id)
            assertEquals(colors.textMuted, parts.itemHeader, theme.id)
            assertEquals(colors.borderSubtle, parts.divider, theme.id)
            assertEquals(colors.dangerSubtle, parts.swipe, theme.id)
            assertEquals(colors.dangerStrong, parts.onSwipe, theme.id)
        }
    }
}
