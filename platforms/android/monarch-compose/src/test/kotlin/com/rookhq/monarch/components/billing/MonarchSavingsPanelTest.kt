@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.billing

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.billing.savingspanel.MonarchSavingsAmount
import com.rookhq.monarch.components.billing.savingspanel.MonarchSavingsCategory
import com.rookhq.monarch.components.billing.savingspanel.MonarchSavingsDecisionCount
import com.rookhq.monarch.components.billing.savingspanel.MonarchSavingsItem
import com.rookhq.monarch.components.billing.savingspanel.MonarchSavingsPanel
import com.rookhq.monarch.components.billing.savingspanel.MonarchSavingsPanelLabels
import com.rookhq.monarch.components.billing.savingspanel.MonarchSavingsPanelTokens
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusRegistry
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
import java.math.BigDecimal
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val July1 = 1_782_864_000_000L

private val Registry = MonarchStatusRegistry(
    mapOf(
        "parked" to MonarchStatusDefinition("Parked", MonarchTone.Warning),
        "let_lapse" to MonarchStatusDefinition("Let lapse", MonarchTone.Danger),
    ),
)
private val Usd = MonarchSavingsAmount(BigDecimal("120"), "USD")
private val Eur = MonarchSavingsAmount(BigDecimal("80"), "EUR")
private val Domain = MonarchSavingsItem(
    id = "net",
    name = "example.net",
    category = "parked",
    dateMillis = July1,
    amount = MonarchSavingsAmount(BigDecimal("40"), "USD"),
    decision = "let_lapse",
)
private val Unpriced = MonarchSavingsItem(id = "org", name = "example.org", unpriced = true)

internal object MonarchSavingsPanelFixtures : ComponentFixtures {
    override val id = "savings-panel"
    override val fixtures = listOf(
        fixture("headline and pressable categories") {
            MonarchSavingsPanel(
                unusedCount = 3,
                costs = listOf(Usd),
                categories = listOf(MonarchSavingsCategory("parked", 2)),
                registry = Registry,
                onCategoryClick = {},
            )
        },
        fixture("decisions and savings") {
            MonarchSavingsPanel(
                unusedCount = 2,
                unpricedCount = 1,
                decisions = listOf(MonarchSavingsDecisionCount("keep", "Keep", 1)),
                potentialSavings = listOf(Usd, Eur),
            )
        },
        fixture("pressable items and more") {
            MonarchSavingsPanel(unusedCount = 4, items = listOf(Domain, Unpriced), maxItems = 1, onItemClick = {})
        },
        fixture("loading") { MonarchSavingsPanel(unusedCount = 3, loading = true) },
        fixture("failed") { MonarchSavingsPanel(unusedCount = 3, error = "No connection.", onRetry = {}) },
        fixture("empty") { MonarchSavingsPanel(unusedCount = 0) },
        fixture("long text") {
            MonarchSavingsPanel(
                unusedCount = 1,
                title = "Savings across every workspace and every billing account",
                items = listOf(MonarchSavingsItem("x", "A very long asset name that wraps onto several lines")),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSavingsPanelTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchSavingsPanelTokens

    @Test
    fun amountsAreListedPerCurrencyAndNeverAdded() {
        assertEquals("$120.00 + €80.00", tokens.join(listOf(Usd, Eur), Locale.US))
        assertNull(tokens.join(emptyList(), Locale.US))
        val (shown, hidden) = tokens.shownItems(listOf(Domain, Unpriced), 1)
        assertEquals(listOf("net"), shown.map { it.id })
        assertEquals(1, hidden)
        assertEquals(2, tokens.shownItems(listOf(Domain, Unpriced), -1).second)
        val categories = listOf(MonarchSavingsCategory("parked", 2), MonarchSavingsCategory("x", 0))
        assertEquals(listOf("parked"), tokens.shownCategories(categories).map { it.id })
    }

    @Test
    fun theHeadlineCountsCostsAndUnpricedItems() {
        rule.setMonarchContent {
            MonarchSavingsPanel(unusedCount = 3, costs = listOf(Usd, Eur), unpricedCount = 1, locale = Locale.US)
        }
        rule.onNodeWithText("3 unused items cost $120.00 + €80.00 to renew (1 without a price).").assertExists()
        rule.onNodeWithText("Savings", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aSingleItemWithoutCostsHasAShortHeadline() {
        rule.setMonarchContent { MonarchSavingsPanel(unusedCount = 1) }
        rule.onNodeWithText("1 unused item.").assertExists()
    }

    @Test
    fun categoriesAreReadWithTheirCountsAndCanBePressed() {
        var opened: String? = null
        rule.setMonarchContent {
            MonarchSavingsPanel(
                unusedCount = 3,
                categories = listOf(
                    MonarchSavingsCategory("parked", 2),
                    MonarchSavingsCategory("no_website", 1, label = "No site"),
                    MonarchSavingsCategory("empty", 0),
                ),
                registry = Registry,
                onCategoryClick = { opened = it.id },
            )
        }
        rule.onNodeWithContentDescription("No site, 1").assertExists()
        rule.onNodeWithContentDescription("Empty, 0").assertDoesNotExist()
        rule.onNodeWithContentDescription("Parked, 2")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("parked", opened)
    }

    @Test
    fun itemsAreReadInWordsAndTheRestAreCounted() {
        var opened: String? = null
        rule.setMonarchContent {
            MonarchSavingsPanel(
                unusedCount = 5,
                items = listOf(Domain, Unpriced, MonarchSavingsItem("z", "Z")),
                maxItems = 2,
                registry = Registry,
                locale = Locale.US,
                onItemClick = { opened = it.id },
            )
        }
        rule.onNodeWithContentDescription("example.net, Parked, Jul 1, 2026, $40.00, Let lapse")
            .assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithContentDescription("example.org, No price").performClick()
        assertEquals("org", opened)
        rule.onNodeWithText("and 1 more").assertExists()
    }

    @Test
    fun decisionsAndSavingsAreWritten() {
        rule.setMonarchContent {
            MonarchSavingsPanel(
                unusedCount = 2,
                decisions = listOf(
                    MonarchSavingsDecisionCount("keep", "Keep", 1),
                    MonarchSavingsDecisionCount("review", "Review", 1),
                ),
                potentialSavings = listOf(Usd),
                locale = Locale.US,
            )
        }
        rule.onNodeWithContentDescription("Decisions: Keep 1 · Review 1").assertExists()
        rule.onNodeWithText("Letting the marked items lapse saves $120.00.").assertExists()
    }

    @Test
    fun errorWinsOverLoadingAndRetries() {
        var retries = 0
        rule.setMonarchContent {
            MonarchSavingsPanel(unusedCount = 2, loading = true, error = "", onRetry = { retries++ })
        }
        rule.onNodeWithText("Savings could not be loaded.", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun loadingAndEmptyStatesSaySo() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchSavingsPanel(unusedCount = 2, loading = true) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Loading savings", substring = true).assertExists()
    }

    @Test
    fun theEmptyStateSaysNothingIsUnused() {
        rule.setMonarchContent { MonarchSavingsPanel(unusedCount = 0, items = listOf(Domain)) }
        rule.onNodeWithText("Nothing unused", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("example.net", substring = true).assertDoesNotExist()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchSavingsPanel(
                unusedCount = 2,
                items = listOf(Domain, Unpriced),
                maxItems = 1,
                labels = MonarchSavingsPanelLabels(
                    title = "Ahorro",
                    headline = { count, _, _ -> "$count sin uso" },
                    more = { "y $it más" },
                ),
            )
        }
        rule.onNodeWithText("Ahorro", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("2 sin uso").assertExists()
        rule.onNodeWithText("y 1 más").assertExists()
    }

    @Test
    fun rightToLeftPutsTheDateBeforeTheAmount() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchSavingsPanel(unusedCount = 1, items = listOf(Domain), locale = Locale.US)
        }
        rule.onNodeWithText("Jul 1, 2026", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("$40.00", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun theHeadlineWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchSavingsPanel(unusedCount = 3, costs = listOf(Usd), unpricedCount = 1, locale = Locale.US)
        }
        rule.onNodeWithText("3 unused items cost $120.00 to renew (1 without a price).").assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, tokens.mutedColor(colors))
            assertEquals(colors.textDefault, tokens.savingsColor(colors))
        }
    }
}
