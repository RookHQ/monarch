package com.rookhq.monarch.components.billing

import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.billing.invoiceview.InvoiceViewMath
import com.rookhq.monarch.components.billing.invoiceview.InvoiceViewPrinting
import com.rookhq.monarch.components.billing.invoiceview.MonarchInvoiceDetail
import com.rookhq.monarch.components.billing.invoiceview.MonarchInvoiceDiscount
import com.rookhq.monarch.components.billing.invoiceview.MonarchInvoiceLineItem
import com.rookhq.monarch.components.billing.invoiceview.MonarchInvoiceParty
import com.rookhq.monarch.components.billing.invoiceview.MonarchInvoiceTotals
import com.rookhq.monarch.components.billing.invoiceview.MonarchInvoiceView
import com.rookhq.monarch.components.billing.invoiceview.MonarchInvoiceViewLabels
import com.rookhq.monarch.components.billing.invoiceview.MonarchInvoiceViewTokens
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val Issued = 1_780_272_000_000L
private const val Due = 1_781_481_600_000L

private val Workshop =
    MonarchInvoiceLineItem("1", "Discovery workshop", BigDecimal(2), BigDecimal("400.00"), "Two sessions")
private val Support = MonarchInvoiceLineItem("2", "Support hours", BigDecimal("6.5"), BigDecimal("80.00"))
private val Sent = MonarchStatusDefinition("Sent", MonarchTone.Info)

internal object MonarchInvoiceViewFixtures : ComponentFixtures {
    override val id = "invoice-view"
    override val fixtures = listOf(
        fixture("one line, worked-out totals") {
            MonarchInvoiceView(items = listOf(Workshop), currencyCode = "USD", number = "INV-1", locale = Locale.US)
        },
        fixture("print button and status") {
            MonarchInvoiceView(
                items = emptyList(),
                currencyCode = "USD",
                number = "INV-2",
                status = Sent,
                showPrintButton = true,
                onPrint = {},
            )
        },
        fixture("parties and dates") {
            MonarchInvoiceView(
                items = emptyList(),
                currencyCode = "EUR",
                issueDateMillis = Issued,
                dueDateMillis = Due,
                from = MonarchInvoiceParty("Studio A", listOf("Street address")),
                to = MonarchInvoiceParty("Client B"),
            )
        },
        fixture("tax, discount, paid and due") {
            MonarchInvoiceView(
                items = listOf(Support),
                currencyCode = "INR",
                taxRate = BigDecimal(18),
                discount = MonarchInvoiceDiscount.Amount(BigDecimal(20)),
                totals = MonarchInvoiceTotals(amountPaid = BigDecimal(100), amountDue = BigDecimal(441)),
            )
        },
        fixture("long text and notes") {
            MonarchInvoiceView(
                items = listOf(
                    MonarchInvoiceLineItem(
                        "x",
                        "Quarterly retainer for design system maintenance and accessibility reviews",
                        BigDecimal.ONE,
                        BigDecimal("12500.00"),
                    ),
                ),
                currencyCode = "USD",
                title = "Final settlement statement",
                notes = "Pay by bank transfer within fourteen days, quoting the invoice number.",
            )
        },
        fixture("empty") { MonarchInvoiceView(items = emptyList(), currencyCode = "JPY") },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchInvoiceViewTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchInvoiceViewTokens
    private val math = InvoiceViewMath

    @Test
    fun totalsAreWorkedOutLikeTheWebLineItemsEditor() {
        val figures = math.figures(
            listOf(Workshop, Support),
            "USD",
            BigDecimal(10),
            MonarchInvoiceDiscount.Percent(BigDecimal(5)),
        )
        assertEquals(listOf(BigDecimal("800.00"), BigDecimal("520.00")), figures.lines)
        assertEquals(BigDecimal("1320.00"), figures.subtotal)
        assertEquals(BigDecimal("66.00"), figures.discount)
        assertEquals(BigDecimal("125.40"), figures.tax)
        assertEquals(BigDecimal("1379.40"), figures.total)
    }

    @Test
    fun linesRoundHalfAwayFromZeroAndTheDiscountNeverExceedsTheSubtotal() {
        val cent = MonarchInvoiceLineItem("a", "A", BigDecimal.ONE, BigDecimal("1.005"))
        val usd = math.figures(listOf(cent), "USD", null, MonarchInvoiceDiscount.Amount(BigDecimal(50)))
        assertEquals(BigDecimal("1.01"), usd.lines.single())
        assertEquals(BigDecimal("1.01"), usd.discount)
        assertEquals(BigDecimal("0.00"), usd.total)
        val yenItem = MonarchInvoiceLineItem("b", "B", BigDecimal(3), BigDecimal("33.5"))
        val yen = math.figures(listOf(yenItem), "JPY", null, null)
        assertEquals(BigDecimal("101"), yen.total)
        val capped = math.figures(listOf(Workshop), "USD", null, MonarchInvoiceDiscount.Percent(BigDecimal(150)))
        assertEquals(BigDecimal("800.00"), capped.discount)
    }

    @Test
    fun compactLinesWriteTheirFiguresInline() {
        rule.setMonarchContent {
            MonarchInvoiceView(items = listOf(Workshop), currencyCode = "USD", locale = Locale.US)
        }
        rule.onNodeWithText("Qty 2 · Unit price $400.00", useUnmergedTree = true).assertExists()
    }

    @Test
    fun eachLineIsOneElementReadWithItsLabelledFigures() {
        rule.setMonarchContent {
            MonarchInvoiceView(items = listOf(Workshop), currencyCode = "USD", number = "INV-0042", locale = Locale.US)
        }
        rule.onNodeWithContentDescription(
            "Discovery workshop, Two sessions, Qty 2, Unit price $400.00, Amount $800.00",
        ).assertExists()
        rule.onNodeWithText("Invoice INV-0042")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Line items").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun theTotalsShowTheRateTheDiscountAndWhatWasGiven() {
        rule.setMonarchContent {
            MonarchInvoiceView(
                items = listOf(Workshop, Support),
                currencyCode = "USD",
                locale = Locale.US,
                taxRate = BigDecimal(10),
                discount = MonarchInvoiceDiscount.Percent(BigDecimal(5)),
                totals = MonarchInvoiceTotals(total = BigDecimal("1400.00"), amountDue = BigDecimal("1000.00")),
            )
        }
        rule.onNodeWithText("Tax (10%)", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("$66.00", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("$1,400.00", substring = false, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("$1,379.40", useUnmergedTree = true).assertDoesNotExist()
        rule.onNode(hasText("Amount due") and hasText("$1,000.00")).assertExists()
        rule.onNodeWithText("Amount paid", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun aZeroDiscountAndAGivenTaxHideTheRowAndTheRate() {
        val discounted = math.figures(listOf(Workshop), "USD", null, MonarchInvoiceDiscount.Amount(BigDecimal.TEN))
        assertTrue(math.discountShown(MonarchInvoiceTotals(), discounted))
        assertFalse(math.discountShown(MonarchInvoiceTotals(discount = BigDecimal.ZERO), discounted))
        assertTrue(math.taxShown(MonarchInvoiceTotals(tax = BigDecimal.ONE), null))
        assertFalse(math.rateShown(MonarchInvoiceTotals(tax = BigDecimal.ONE), BigDecimal.TEN))
        rule.setMonarchContent {
            MonarchInvoiceView(items = listOf(Workshop), currencyCode = "USD", locale = Locale.US)
        }
        rule.onNodeWithText("Discount", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("Tax", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun thePrintButtonCallsOnPrintAndHasA48dpTarget() {
        var printed = 0
        rule.setMonarchContent {
            MonarchInvoiceView(
                items = emptyList(),
                currencyCode = "USD",
                showPrintButton = true,
                onPrint = { printed++ },
            )
        }
        rule.onNodeWithText("Print").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, printed)
    }

    @Test
    fun withoutShowPrintButtonThereIsNoPrintButton() {
        rule.setMonarchContent { MonarchInvoiceView(items = emptyList(), currencyCode = "USD", onPrint = {}) }
        rule.onNodeWithText("Print").assertDoesNotExist()
        rule.onNodeWithText("No line items.").assertExists()
    }

    @Test
    fun partiesAndDatesAreShown() {
        rule.setMonarchContent {
            MonarchInvoiceView(
                items = emptyList(),
                currencyCode = "USD",
                issueDateMillis = Issued,
                dueDateMillis = Due,
                details = listOf(MonarchInvoiceDetail("Payment terms", "Net 14")),
                from = MonarchInvoiceParty("Studio A", listOf("Street address")),
                to = MonarchInvoiceParty("Client B"),
                locale = Locale.US,
            )
        }
        rule.onNodeWithText("From").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Bill to").assertExists()
        rule.onNodeWithText("June 1, 2026", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("June 15, 2026", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Net 14", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun wideLayoutsShowColumnHeadingsHiddenFromTalkBack() {
        rule.setMonarchContent {
            MonarchInvoiceView(
                items = listOf(Workshop),
                currencyCode = "USD",
                modifier = Modifier.requiredWidth(700.dp),
                locale = Locale.US,
            )
        }
        rule.onNodeWithText("Description", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Description").assertDoesNotExist()
        rule.onNodeWithText("2", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Qty 2 · Unit price $400.00", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithContentDescription("Discovery workshop", substring = true).assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchInvoiceView(
                items = listOf(Workshop),
                currencyCode = "USD",
                locale = Locale.US,
                taxRate = BigDecimal(21),
                labels = MonarchInvoiceViewLabels(
                    title = "Factura",
                    quantity = "Cant.",
                    taxWithRate = { "IVA ($it)" },
                ),
            )
        }
        rule.onNodeWithText("Factura").assertExists()
        rule.onNodeWithText("IVA (21%)", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Cant. 2", substring = true).assertExists()
    }

    @Test
    fun rightToLeftPutsTheLabelBeforeTheAmount() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchInvoiceView(
                items = listOf(Workshop, Support),
                currencyCode = "USD",
                locale = Locale.US,
                taxRate = BigDecimal.TEN,
            )
        }
        rule.onNodeWithText("Subtotal", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("$1,320.00", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun theTitleAndLinesGrowAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchInvoiceView(items = listOf(Workshop), currencyCode = "USD", number = "INV-0042", locale = Locale.US)
        }
        rule.onNodeWithText("Invoice INV-0042").assertTextFullyVisible()
        rule.onNodeWithText("Discovery workshop", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, tokens.mutedColor(colors))
        }
    }

    @Test
    fun aLongPictureIsSplitIntoPages() {
        assertEquals(1, InvoiceViewPrinting.pageCount(100, 150, 50f, 100f))
        assertEquals(2, InvoiceViewPrinting.pageCount(100, 250, 50f, 100f))
        assertEquals(1, InvoiceViewPrinting.pageCount(0, 0, 50f, 100f))
    }

    @Test
    fun quantitiesAndRatesFollowTheLocale() {
        assertEquals("6,5", tokens.quantityText(BigDecimal("6.5"), Locale.GERMANY))
        assertEquals("12.375%", tokens.rateText(BigDecimal("12.375"), Locale.US))
        assertEquals("Invoice", tokens.heading("Invoice", " "))
    }
}
