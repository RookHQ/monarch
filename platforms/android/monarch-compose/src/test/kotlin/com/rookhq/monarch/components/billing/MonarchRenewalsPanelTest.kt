@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.billing

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.billing.renewalspanel.MonarchRenewalFilter
import com.rookhq.monarch.components.billing.renewalspanel.MonarchRenewalItem
import com.rookhq.monarch.components.billing.renewalspanel.MonarchRenewalTotal
import com.rookhq.monarch.components.billing.renewalspanel.MonarchRenewalsPanel
import com.rookhq.monarch.components.billing.renewalspanel.MonarchRenewalsPanelLabels
import com.rookhq.monarch.components.billing.renewalspanel.MonarchRenewalsPanelTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import java.math.BigDecimal
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val Now = 1_782_864_000_000L
private const val Day = 86_400_000L

private val Acme = MonarchRenewalItem(
    id = "acme",
    name = "Acme contract",
    status = "open",
    renewalDateMillis = Now + 12 * Day,
    amount = BigDecimal("12000"),
    currencyCode = "USD",
    amountNote = "per year",
    autoRenew = false,
)
private val Domain = MonarchRenewalItem(
    id = "dom",
    name = "example.com",
    status = "renewed",
    renewalDateMillis = Now - 3 * Day,
    outcome = "Renewed for two years",
)
private val Licence = MonarchRenewalItem(
    id = "lic",
    name = "Design license",
    status = "in_progress",
    renewalDateMillis = Now + 40 * Day,
    noticeDeadlineMillis = Now + 10 * Day,
    owner = "Ana",
    nextAction = "Send a quote",
    nextActionOverdue = true,
    probability = 60,
)

internal object MonarchRenewalsPanelFixtures : ComponentFixtures {
    override val id = "renewals-panel"
    override val fixtures = listOf(
        fixture("an urgent row with an action") {
            MonarchRenewalsPanel(items = listOf(Acme), now = Now, onItemAction = {})
        },
        fixture("pressable rows") {
            MonarchRenewalsPanel(items = listOf(Domain), now = Now, onItemClick = {})
        },
        fixture("owner, notice and probability") {
            MonarchRenewalsPanel(items = listOf(Licence), now = Now)
        },
        fixture("filters") {
            MonarchRenewalsPanel(
                items = emptyList(),
                filters = listOf(MonarchRenewalFilter("30", "30 days", 4), MonarchRenewalFilter("all", "All")),
                onFilterChange = {},
            )
        },
        fixture("totals") {
            MonarchRenewalsPanel(
                items = emptyList(),
                totals = listOf(MonarchRenewalTotal("USD", BigDecimal("1200"), 2, BigDecimal("200"))),
                unpricedCount = 1,
            )
        },
        fixture("loading") { MonarchRenewalsPanel(items = emptyList(), loading = true) },
        fixture("failed") { MonarchRenewalsPanel(items = emptyList(), error = "No connection.", onRetry = {}) },
        fixture("long text") {
            MonarchRenewalsPanel(
                items = listOf(
                    MonarchRenewalItem(
                        id = "long",
                        name = "Enterprise agreement for analytics and data warehousing in the northern region",
                        status = "void",
                        renewalDateMillis = null,
                    ),
                ),
                title = "Renewals and expiries for every customer in the portfolio",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchRenewalsPanelTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchRenewalsPanelTokens

    @Test
    fun daysAreWorkedOutAndOpenItemsAreUrgent() {
        assertEquals(12, tokens.daysUntil(Acme, Now))
        assertEquals(-3, tokens.daysUntil(Domain, Now))
        assertEquals(5, tokens.daysUntil(MonarchRenewalItem("x", "X", "open", null, daysUntil = 5), Now))
        assertNull(tokens.daysUntil(MonarchRenewalItem("x", "X", "open", null), Now))
        assertTrue(tokens.urgent(Acme, 12, 14))
        assertFalse(tokens.urgent(Acme, 15, 14))
        assertFalse(tokens.urgent(Domain, -3, 14))
        assertTrue(tokens.urgent(Licence, -1, 14))
    }

    @Test
    fun eachRowIsOneElementWithItsDatesAndAmount() {
        rule.setMonarchContent {
            MonarchRenewalsPanel(items = listOf(Acme, Domain), locale = Locale.US, now = Now)
        }
        rule.onNodeWithContentDescription(
            "Acme contract, Open, Due soon, Jul 13, 2026 · in 12 days, $12,000.00, per year · Manual renewal",
        ).assertExists()
        rule.onNodeWithContentDescription(
            "example.com, Renewed, Jun 28, 2026 · 3 days ago, No price, Renewed for two years",
        ).assertExists()
        rule.onNodeWithContentDescription("Renewals")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo))
    }

    @Test
    fun theNoticeDeadlineOwnerAndOverdueStepAreWritten() {
        rule.setMonarchContent { MonarchRenewalsPanel(items = listOf(Licence), locale = Locale.US, now = Now) }
        rule.onNodeWithText("Aug 10, 2026", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Notice by Jul 11, 2026", useUnmergedTree = true).assertExists()
        rule.onNode(hasContentDescription("Overdue: Send a quote", substring = true)).assertExists()
        rule.onNodeWithText("60% likely", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theRowActionIsNamedAfterTheItemAndCanBeHidden() {
        val acted = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchRenewalsPanel(
                items = listOf(Acme, Domain),
                now = Now,
                onItemAction = { acted += it.id },
                canAct = { it.status == "open" },
            )
        }
        rule.onNodeWithContentDescription("Update Acme contract").performScrollTo()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithContentDescription("Update example.com").assertDoesNotExist()
        assertEquals(listOf("acme"), acted)
    }

    @Test
    fun pressableRowsAreButtons() {
        var opened: String? = null
        rule.setMonarchContent {
            MonarchRenewalsPanel(items = listOf(Domain), now = Now, onItemClick = { opened = it.id })
        }
        rule.onNodeWithContentDescription("example.com", substring = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("dom", opened)
    }

    @Test
    fun filtersAreSegmentsAndReportTheChoice() {
        var chosen: String? = null
        rule.setMonarchContent {
            MonarchRenewalsPanel(
                items = emptyList(),
                filters = listOf(MonarchRenewalFilter("30", "30 days", 4), MonarchRenewalFilter("all", "All")),
                onFilterChange = { chosen = it },
            )
        }
        rule.onNodeWithText("30 days (4)").assertIsSelected()
        rule.onNodeWithText("All").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals("all", chosen)
    }

    @Test
    fun totalsAreReadPerCurrency() {
        rule.setMonarchContent {
            MonarchRenewalsPanel(
                items = emptyList(),
                totals = listOf(MonarchRenewalTotal("USD", BigDecimal("1200"), 2, BigDecimal("200"))),
                unpricedCount = 3,
                locale = Locale.US,
            )
        }
        rule.onNode(hasText("USD · 2 renewals") and hasText("$1,200.00") and hasText("Includes $200.00 estimated"))
            .assertExists()
        rule.onNode(hasText("Without a price") and hasText("3") and hasText("Not included in the totals."))
            .assertExists()
    }

    @Test
    fun loadingErrorAndEmptyStates() {
        var retries = 0
        rule.setMonarchContent {
            MonarchRenewalsPanel(
                items = listOf(Acme),
                error = "No connection.",
                loading = true,
                onRetry = { retries++ },
            )
        }
        rule.onNodeWithText("Renewals could not be loaded.", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Acme contract", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun theLoadingAndEmptyStatesSaySo() {
        rule.setMonarchContent { MonarchRenewalsPanel(items = emptyList()) }
        rule.onNodeWithText("No renewals coming up", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theLoadingStateIsNamed() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchRenewalsPanel(items = listOf(Acme), loading = true) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Loading renewals", substring = true).assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchRenewalsPanel(
                items = listOf(Acme),
                now = Now,
                onItemAction = {},
                labels = MonarchRenewalsPanelLabels(
                    title = "Renovaciones",
                    dueSoon = "Pronto",
                    daysUntil = { "en $it días" },
                    action = "Actualizar",
                ),
            )
        }
        rule.onNodeWithText("Renovaciones", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Acme contract, Open, Pronto", substring = true).assertExists()
        rule.onNodeWithContentDescription("en 12 días", substring = true).assertExists()
        rule.onNodeWithContentDescription("Actualizar Acme contract").assertExists()
    }

    @Test
    fun rightToLeftPutsTheAmountBeforeItsNote() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchRenewalsPanel(items = listOf(Acme), now = Now, locale = Locale.US)
        }
        rule.onNodeWithText("$12,000.00", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("per year", useUnmergedTree = true).assertExists()
    }

    @Test
    fun namesWrapAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchRenewalsPanel(items = listOf(Acme), now = Now)
        }
        rule.onNodeWithText("Acme contract", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, tokens.mutedColor(colors))
            assertEquals(colors.dangerStrong, tokens.overdueColor(colors))
            assertEquals(colors.bgSubtle, tokens.tileColor(colors))
        }
    }
}
