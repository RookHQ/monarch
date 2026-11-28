package com.rookhq.monarch.components.billing

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.billing.payslipview.MonarchPayslipDetail
import com.rookhq.monarch.components.billing.payslipview.MonarchPayslipLine
import com.rookhq.monarch.components.billing.payslipview.MonarchPayslipLineKind
import com.rookhq.monarch.components.billing.payslipview.MonarchPayslipTotals
import com.rookhq.monarch.components.billing.payslipview.MonarchPayslipView
import com.rookhq.monarch.components.billing.payslipview.MonarchPayslipViewLabels
import com.rookhq.monarch.components.billing.payslipview.MonarchPayslipViewTokens
import com.rookhq.monarch.components.billing.payslipview.MonarchPayslipYearToDateItem
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
import com.rookhq.monarch.testing.stateDescription
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

private val Basic =
    MonarchPayslipLine("basic", "Basic", MonarchPayslipLineKind.Earning, BigDecimal("4000.00"), "22 of 22 days")
private val Tax = MonarchPayslipLine("tax", "Income tax", MonarchPayslipLineKind.Deduction, BigDecimal("640.00"))
private val Travel = MonarchPayslipLine("travel", "Travel", MonarchPayslipLineKind.Reimbursement, BigDecimal("35.00"))
private val Pension = MonarchPayslipLine("pension", "Pension", MonarchPayslipLineKind.Employer, BigDecimal("200.00"))
private val Totals = MonarchPayslipTotals(
    gross = BigDecimal("4000.00"),
    deductions = BigDecimal("640.00"),
    net = BigDecimal("3395.00"),
    reimbursements = BigDecimal("35.00"),
)
private val Zero = MonarchPayslipTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)

internal object MonarchPayslipViewFixtures : ComponentFixtures {
    override val id = "payslip-view"
    override val fixtures = listOf(
        fixture("download, status and one line") {
            MonarchPayslipView(
                title = "Payslip",
                lines = listOf(Basic),
                totals = Zero,
                currencyCode = "USD",
                status = MonarchStatusDefinition("Paid", MonarchTone.Success),
                onDownload = {},
            )
        },
        fixture("downloading") {
            MonarchPayslipView(
                title = "Payslip",
                lines = emptyList(),
                totals = Zero,
                currencyCode = "USD",
                downloading = true,
                onDownload = {},
            )
        },
        fixture("deductions and reimbursements") {
            MonarchPayslipView(title = "Payslip", lines = listOf(Tax, Travel), totals = Totals, currencyCode = "EUR")
        },
        fixture("issuer, subtitle and details") {
            MonarchPayslipView(
                title = "Payslip",
                lines = emptyList(),
                totals = Zero,
                currencyCode = "GBP",
                issuer = "Studio A Ltd",
                subtitle = "June 2026",
                details = listOf(MonarchPayslipDetail("Employee", "Ana Silva")),
            )
        },
        fixture("year to date, employer hidden") {
            MonarchPayslipView(
                title = "Payslip",
                lines = listOf(Pension),
                totals = Zero,
                currencyCode = "INR",
                hideEmployerContributions = true,
                yearToDate = listOf(MonarchPayslipYearToDateItem("Gross", BigDecimal("12000"))),
            )
        },
        fixture("long text") {
            MonarchPayslipView(
                title = "Final settlement after resignation with notice period buy-out",
                lines = listOf(
                    MonarchPayslipLine(
                        "leave",
                        "Encashment of unused earned leave carried over from previous years",
                        MonarchPayslipLineKind.Earning,
                        BigDecimal("123456.78"),
                    ),
                ),
                totals = Zero,
                currencyCode = "INR",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchPayslipViewTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchPayslipViewTokens

    @Test
    fun linesAreGroupedInOrderWithoutEmptyGroups() {
        val groups = tokens.groups(listOf(Pension, Tax, Basic), hideEmployer = false)
        assertEquals(
            listOf(MonarchPayslipLineKind.Earning, MonarchPayslipLineKind.Deduction, MonarchPayslipLineKind.Employer),
            groups.map { it.first },
        )
        assertEquals(
            listOf(MonarchPayslipLineKind.Earning, MonarchPayslipLineKind.Deduction),
            tokens.groups(listOf(Pension, Tax, Basic), hideEmployer = true).map { it.first },
        )
    }

    @Test
    fun groupNamesAreHeadingsAndEachLineIsOneElement() {
        rule.setMonarchContent {
            MonarchPayslipView(
                title = "Payslip",
                lines = listOf(Basic, Tax, Travel, Pension),
                totals = Totals,
                currencyCode = "USD",
                locale = Locale.US,
            )
        }
        rule.onNodeWithText("Payslip").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        listOf("Earnings", "Deductions", "Employer contributions").forEach {
            rule.onNode(hasText(it) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()
        }
        rule.onNode(hasText("Basic") and hasText("22 of 22 days") and hasText("$4,000.00")).assertExists()
        rule.onNode(hasText("Net pay") and hasText("$3,395.00")).assertExists()
        rule.onNode(hasText("Reimbursements") and hasText("$35.00")).assertExists()
    }

    @Test
    fun employerContributionsCanBeHiddenAndZeroReimbursementsAreNotShown() {
        assertFalse(tokens.reimbursementsShown(Zero))
        val zeroReimbursed = MonarchPayslipTotals(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO)
        assertFalse(tokens.reimbursementsShown(zeroReimbursed))
        assertTrue(tokens.reimbursementsShown(Totals))
        rule.setMonarchContent {
            MonarchPayslipView(
                title = "Payslip",
                lines = listOf(Basic, Pension),
                totals = Zero,
                currencyCode = "USD",
                hideEmployerContributions = true,
            )
        }
        rule.onNodeWithText("Employer contributions", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("Pension", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("Reimbursements", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun noLinesSaysSo() {
        rule.setMonarchContent {
            MonarchPayslipView(title = "Payslip", lines = emptyList(), totals = Zero, currencyCode = "USD")
        }
        rule.onNodeWithText("No pay lines.").assertExists()
        rule.onNodeWithText("Download PDF").assertDoesNotExist()
    }

    @Test
    fun downloadCallsBackAndShowsItsLoadingState() {
        var downloads = 0
        rule.setMonarchContent {
            MonarchPayslipView(
                title = "Payslip",
                lines = emptyList(),
                totals = Zero,
                currencyCode = "USD",
                onDownload = { downloads++ },
            )
        }
        rule.onNodeWithText("Download PDF").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, downloads)
    }

    @Test
    fun whileDownloadingTheButtonIsBusy() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchPayslipView(
                title = "Payslip",
                lines = emptyList(),
                totals = Zero,
                currencyCode = "USD",
                downloading = true,
                onDownload = {},
            )
        }
        rule.mainClock.advanceTimeByFrame()
        val button = rule.onNodeWithText("Download PDF")
        button.assertIsNotEnabled()
        assertEquals("Preparing PDF", button.stateDescription())
    }

    @Test
    fun yearToDateTilesAndAReplacedTitle() {
        rule.setMonarchContent {
            MonarchPayslipView(
                title = "Payslip",
                lines = emptyList(),
                totals = Zero,
                currencyCode = "USD",
                locale = Locale.US,
                yearToDate = listOf(MonarchPayslipYearToDateItem("Gross", BigDecimal("12000"))),
                yearToDateTitle = "Year to date (from April)",
            )
        }
        rule.onNode(hasText("Year to date (from April)") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertExists()
        rule.onNode(hasText("Gross") and hasText("$12,000.00")).assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchPayslipView(
                title = "Nómina",
                lines = listOf(Basic),
                totals = Zero,
                currencyCode = "EUR",
                onDownload = {},
                labels = MonarchPayslipViewLabels(earnings = "Devengos", netPay = "Líquido", download = "Descargar"),
            )
        }
        rule.onNode(hasText("Devengos") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()
        rule.onNodeWithText("Líquido", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Descargar").assertExists()
    }

    @Test
    fun rightToLeftPutsTheNameBeforeTheAmount() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchPayslipView(
                title = "Payslip",
                lines = listOf(Tax),
                totals = Zero,
                currencyCode = "USD",
                locale = Locale.US,
            )
        }
        rule.onNodeWithText("Income tax", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("$640.00", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun longNamesWrapAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchPayslipView(title = "Final settlement", lines = listOf(Basic), totals = Zero, currencyCode = "USD")
        }
        rule.onNodeWithText("Final settlement").assertTextFullyVisible()
        rule.onNodeWithText("22 of 22 days", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, tokens.mutedColor(colors))
            assertEquals(colors.bgSubtle, tokens.tileColor(colors))
        }
    }
}
