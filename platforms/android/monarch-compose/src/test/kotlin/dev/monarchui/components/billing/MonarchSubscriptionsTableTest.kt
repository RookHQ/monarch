package dev.monarchui.components.billing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.billing.subscriptionstable.MonarchSubscription
import dev.monarchui.components.billing.subscriptionstable.MonarchSubscriptionInterval
import dev.monarchui.components.billing.subscriptionstable.MonarchSubscriptionMonthly
import dev.monarchui.components.billing.subscriptionstable.MonarchSubscriptionsTable
import dev.monarchui.components.billing.subscriptionstable.MonarchSubscriptionsTableLabels
import dev.monarchui.components.billing.subscriptionstable.MonarchSubscriptionsTableTokens
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusRegistry
import dev.monarchui.foundation.MonarchTone
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
import java.math.BigDecimal
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val July1 = 1_782_864_000_000L
private const val March1 = 1_803_859_200_000L

private val Team = MonarchSubscription(
    id = "team",
    name = "Acme workspace",
    status = "active",
    amount = BigDecimal("49.00"),
    currencyCode = "USD",
    plan = "Team plan",
    quantity = 3,
    customer = "Acme Ltd",
    monthly = MonarchSubscriptionMonthly.Amount(BigDecimal("147.00"), "USD"),
    renewalDateMillis = July1,
    cancelsAtMillis = March1,
)
private val Support = MonarchSubscription(
    id = "support",
    name = "Globex support",
    status = "past_due",
    amount = BigDecimal("1200.00"),
    currencyCode = "EUR",
    interval = MonarchSubscriptionInterval.Year,
    monthly = MonarchSubscriptionMonthly.NotConverted,
    renewalDateMillis = July1,
    renewsAutomatically = false,
)
private val Setup = MonarchSubscription(
    id = "setup",
    name = "Onboarding",
    status = "draft",
    amount = null,
    currencyCode = "USD",
    interval = null,
)

internal object MonarchSubscriptionsTableFixtures : ComponentFixtures {
    override val id = "subscriptions-table"
    override val fixtures = listOf(
        fixture("one row with every part") { MonarchSubscriptionsTable(subscriptions = listOf(Team)) },
        fixture("selectable rows") {
            MonarchSubscriptionsTable(subscriptions = listOf(Support, Setup), onSelect = {})
        },
        fixture("customer and monthly hidden") {
            MonarchSubscriptionsTable(subscriptions = listOf(Team), showCustomer = false, showMonthly = false)
        },
        fixture("every status") {
            MonarchSubscriptionsTable(
                subscriptions = listOf("trial", "paused", "cancelled", "expired").map {
                    MonarchSubscription(id = it, name = it, status = it, amount = BigDecimal.ONE, currencyCode = "USD")
                },
            )
        },
        fixture("long text") {
            MonarchSubscriptionsTable(
                subscriptions = listOf(
                    MonarchSubscription(
                        id = "long",
                        name = "Enterprise analytics workspace for the northern region",
                        status = "active",
                        amount = BigDecimal("123456.78"),
                        currencyCode = "INR",
                        plan = "Enterprise plan with premium support",
                        intervalCount = 3,
                    ),
                ),
            )
        },
        fixture("empty") { MonarchSubscriptionsTable(subscriptions = emptyList()) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSubscriptionsTableTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchSubscriptionsTableTokens

    @Test
    fun eachRowIsReadInWords() {
        rule.setMonarchContent {
            MonarchSubscriptionsTable(subscriptions = listOf(Team, Support, Setup), locale = Locale.US)
        }
        rule.onNodeWithContentDescription(
            "Acme workspace, 3 × Team plan · cancels Mar 1, 2027, Acme Ltd, Active, $49.00 per month, " +
                "Monthly $147.00, Renews Jul 1, 2026",
        ).assertExists()
        rule.onNodeWithContentDescription(
            "Globex support, Custom price, Past due, €1,200.00 per year, Monthly Not converted. " +
                "No exchange rate is available, so this is left out of the monthly total., Ends Jul 1, 2026",
        ).assertExists()
        rule.onNodeWithContentDescription(
            "Onboarding, Custom price, Draft, No value, Monthly No value, Evergreen",
        ).assertExists()
        rule.onNodeWithContentDescription("Subscriptions").assert(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo),
        )
    }

    @Test
    fun theRowsShowThePriceWithItsPeriod() {
        rule.setMonarchContent {
            MonarchSubscriptionsTable(subscriptions = listOf(Team, Setup), locale = Locale.US)
        }
        rule.onNodeWithText("$49.00", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("/ month", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("/ one time", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Acme Ltd", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Renews Jul 1, 2026", useUnmergedTree = true).assertExists()
    }

    @Test
    fun selectableRowsAreButtonsWithA48dpTarget() {
        var selected: String? = null
        rule.setMonarchContent {
            MonarchSubscriptionsTable(subscriptions = listOf(Setup), onSelect = { selected = it.id })
        }
        rule.onNodeWithContentDescription("Onboarding", substring = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("setup", selected)
    }

    @Test
    fun rowsWithoutOnSelectAreNotButtons() {
        rule.setMonarchContent { MonarchSubscriptionsTable(subscriptions = listOf(Setup)) }
        rule.onNodeWithContentDescription("Onboarding", substring = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
    }

    @Test
    fun theMonthlyColumnAndNotConvertedFollowTheRules() {
        assertTrue(tokens.monthlyShown(null, listOf(Team)))
        assertFalse(tokens.monthlyShown(null, listOf(Setup)))
        assertFalse(tokens.monthlyShown(false, listOf(Team)))
        assertTrue(tokens.notConverted(Support))
        val paused = MonarchSubscription(
            id = "p",
            name = "P",
            status = "paused",
            amount = BigDecimal.ONE,
            currencyCode = "USD",
            monthly = MonarchSubscriptionMonthly.NotConverted,
        )
        assertFalse(tokens.notConverted(paused))
    }

    @Test
    fun registryStatusesWinAndUnknownKeysAreReadable() {
        val custom = MonarchStatusRegistry(mapOf("active" to MonarchStatusDefinition("Live", MonarchTone.Success)))
        assertEquals("Live", tokens.status("active", custom, MonarchStatusRegistry.Empty).label)
        val empty = MonarchStatusRegistry.Empty
        assertEquals("On hold", tokens.status("on_hold", empty, empty).label)
        rule.setMonarchContent {
            MonarchSubscriptionsTable(subscriptions = listOf(Team), registry = custom, locale = Locale.US)
        }
        rule.onNodeWithContentDescription("Acme Ltd, Live,", substring = true).assertExists()
    }

    @Test
    fun theEmptyStateSaysSo() {
        rule.setMonarchContent { MonarchSubscriptionsTable(subscriptions = emptyList()) }
        rule.onNodeWithText("No subscriptions", useUnmergedTree = true).assertExists()
    }

    @Test
    fun wideTablesShowHeadingsHiddenFromTalkBack() {
        rule.setMonarchContent {
            MonarchSubscriptionsTable(
                subscriptions = listOf(Team),
                modifier = Modifier.requiredWidth(800.dp),
                locale = Locale.US,
            )
        }
        rule.onNodeWithText("Renewal", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Renewal").assertDoesNotExist()
        rule.onNodeWithText("Acme Ltd", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Acme Ltd · Renews Jul 1, 2026", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchSubscriptionsTable(
                subscriptions = listOf(Setup),
                showMonthly = false,
                labels = MonarchSubscriptionsTableLabels(
                    customPrice = "Precio a medida",
                    evergreen = "Sin fin",
                    noValue = "Sin valor",
                    caption = "Suscripciones",
                ),
            )
        }
        rule.onNodeWithContentDescription("Onboarding, Precio a medida, Draft, Sin valor, Sin fin").assertExists()
        rule.onNodeWithContentDescription("Suscripciones").assertExists()
    }

    @Test
    fun rightToLeftPutsTheNameBeforeThePrice() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchSubscriptionsTable(subscriptions = listOf(Team), locale = Locale.US)
        }
        rule.onNodeWithText("Acme workspace", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("$49.00", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun namesWrapAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Column { MonarchSubscriptionsTable(subscriptions = listOf(Team), locale = Locale.US) }
        }
        rule.onNodeWithText("Acme workspace", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, tokens.mutedColor(colors))
            assertEquals(colors.textMuted, tokens.headingColor(colors))
        }
    }
}
