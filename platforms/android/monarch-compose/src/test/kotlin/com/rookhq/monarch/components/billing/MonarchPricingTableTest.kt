package com.rookhq.monarch.components.billing

import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingFeature
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingFeatureValue
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingPeriod
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingPlan
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingTable
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingTableLabels
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingTableLayout
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingTableTokens
import com.rookhq.monarch.components.billing.pricingtable.PricingPrice
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

private val starter = MonarchPricingPlan("starter", "Starter", prices = mapOf("month" to "$0", "year" to "$0"))
private val team = MonarchPricingPlan(
    "team",
    "Team",
    description = "For growing teams",
    prices = mapOf("month" to "$12", "year" to "$120"),
    badge = "Recommended",
    highlighted = true,
)
private val enterprise = MonarchPricingPlan("enterprise", "Enterprise", price = "Contact us", current = true)
private val plans = listOf(starter, team, enterprise)
private val features = listOf(
    MonarchPricingFeature(
        "seats",
        "Seats",
        values = mapOf(
            "starter" to MonarchPricingFeatureValue.Text("3"),
            "team" to MonarchPricingFeatureValue.Text("Unlimited"),
            "enterprise" to MonarchPricingFeatureValue.Text("Unlimited"),
        ),
    ),
    MonarchPricingFeature(
        "sso",
        "Single sign-on",
        description = "SAML and OIDC",
        group = "Security",
        values = mapOf(
            "team" to MonarchPricingFeatureValue.Included,
            "enterprise" to MonarchPricingFeatureValue.Included,
        ),
    ),
)
private val periods = listOf(
    MonarchPricingPeriod("month", "Monthly", priceSuffix = "/ month"),
    MonarchPricingPeriod(
        "year",
        "Yearly",
        suffix = "2 months included",
        priceSuffix = "/ year",
        priceNote = "Billed yearly",
    ),
)

internal object MonarchPricingTableFixtures : ComponentFixtures {
    override val id = "pricing-table"
    override val fixtures = listOf(
        fixture("paged, periods and plans") {
            MonarchPricingTable(plans = plans.take(2), features = emptyList(), periods = periods)
        },
        fixture("paged, one plan with features") {
            MonarchPricingTable(plans = listOf(team), features = features, layout = MonarchPricingTableLayout.Paged)
        },
        fixture("paged, current plan without prices") {
            MonarchPricingTable(plans = listOf(enterprise), features = features.take(1), showPrices = false)
        },
        fixture("grid") {
            MonarchPricingTable(plans = plans, features = features, layout = MonarchPricingTableLayout.Grid)
        },
        fixture("grid, one plan with an action") {
            MonarchPricingTable(
                plans = listOf(starter),
                features = features.take(1),
                layout = MonarchPricingTableLayout.Grid,
            ) { MonarchButton(text = "Choose", onClick = {}) }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchPricingTableTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aPhoneShowsOnePlanPerPageWithAPlanPicker() {
        rule.setMonarchContent { MonarchPricingTable(plans = plans, features = features, periods = periods) }
        rule.onNodeWithContentDescription("Plan comparison").assertExists()
        rule.onNodeWithContentDescription("Plan").assertExists()
        rule.onNodeWithContentDescription("Seats, 3").assertExists()
        rule.onNodeWithContentDescription("Single sign-on, Not included, SAML and OIDC").assertExists()
        rule.onNode(hasText("Security") and isHeading()).assertExists()
    }

    @Test
    fun choosingAPlanShowsItsPage() {
        rule.setMonarchContent { MonarchPricingTable(plans = plans, features = features, periods = periods) }
        val segment = rule.onNode(
            hasText("Team") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
        )
        segment.assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        segment.assertIsSelected()
        rule.onNodeWithContentDescription("Seats, Unlimited").assertExists()
        rule.onNodeWithContentDescription("Single sign-on, Included, SAML and OIDC").assertExists()
    }

    @Test
    fun theBillingPeriodChangesThePrices() {
        var period by mutableStateOf("month")
        rule.setMonarchContent {
            MonarchPricingTable(
                plans = listOf(team),
                features = emptyList(),
                periods = periods,
                period = period,
                onPeriodChange = { period = it },
            )
        }
        rule.onNodeWithContentDescription("Billing period").assertExists()
        rule.onNodeWithText("$12", substring = true).assertExists()
        rule.onNode(hasText("Yearly") and hasClickAction()).performClick()
        rule.waitForIdle()
        assertEquals("year", period)
        rule.onNodeWithText("$120", substring = true).assertExists()
        rule.onNodeWithText("2 months included").assertExists()
    }

    @Test
    fun withoutAControlledPeriodTheTableKeepsItsOwn() {
        rule.setMonarchContent { MonarchPricingTable(plans = listOf(team), features = emptyList(), periods = periods) }
        rule.onNode(hasText("Yearly") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("$120", substring = true).assertExists()
    }

    @Test
    fun theGridReadsEachCellWithItsPlanAndFeature() {
        rule.setMonarchContent {
            MonarchPricingTable(
                plans = plans,
                features = features,
                modifier = Modifier.requiredWidth(720.dp),
                periods = periods,
            )
        }
        rule.onNodeWithContentDescription("Plan").assertDoesNotExist()
        rule.onNodeWithContentDescription("Team, Single sign-on: Included").assertExists()
        rule.onNodeWithContentDescription("Starter, Single sign-on: Not included").assertExists()
        rule.onNodeWithContentDescription("Starter, Seats: 3").assertExists()
        rule.onNodeWithText("Contact us").assertExists()
        rule.onNodeWithText("Current plan").assertExists()
        rule.onNodeWithText("Recommended").assertExists()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchPricingTable(
                plans = plans,
                features = features,
                periods = periods,
                labels = MonarchPricingTableLabels(
                    caption = "Comparatif",
                    included = "Inclus",
                    notIncluded = "Non inclus",
                    billingPeriod = "Période",
                    plan = "Offre",
                ),
            )
        }
        rule.onNodeWithContentDescription("Comparatif").assertExists()
        rule.onNodeWithContentDescription("Période").assertExists()
        rule.onNodeWithContentDescription("Offre").assertExists()
        rule.onNodeWithContentDescription("Single sign-on, Non inclus, SAML and OIDC").assertExists()
    }

    @Test
    fun segmentsFollowTheLayoutDirection() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchPricingTable(plans = plans.take(2), features = emptyList(), periods = periods)
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Monthly", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Yearly", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun featureTextIsNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchPricingTable(plans = listOf(team), features = features)
        }
        rule.onNodeWithText("Single sign-on", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun pricesFollowThePeriodAndFallBack() {
        val month = periods[0]
        val year = periods[1]
        assertEquals(PricingPrice("$12", "/ month", null), MonarchPricingTableTokens.price(team, month))
        assertEquals(PricingPrice("$120", "/ year", "Billed yearly"), MonarchPricingTableTokens.price(team, year))
        assertEquals(PricingPrice("Contact us", null, null), MonarchPricingTableTokens.price(enterprise, year))
        val perSeat = MonarchPricingPlan("seat", "Seat", prices = mapOf("month" to "$8"), priceSuffix = "per seat")
        assertEquals(PricingPrice("$8", "per seat", null), MonarchPricingTableTokens.price(perSeat, month))
        assertEquals(year, MonarchPricingTableTokens.selectedPeriod("year", periods))
        assertEquals(month, MonarchPricingTableTokens.selectedPeriod("decade", periods))
    }

    @Test
    fun theLayoutAndGroupsFollowTheRules() {
        val automatic = MonarchPricingTableLayout.Automatic
        assertEquals(MonarchPricingTableLayout.Paged, MonarchPricingTableTokens.layout(automatic, 360f, 1f))
        assertEquals(MonarchPricingTableLayout.Grid, MonarchPricingTableTokens.layout(automatic, 800f, 1f))
        assertEquals(MonarchPricingTableLayout.Paged, MonarchPricingTableTokens.layout(automatic, 800f, 2f))
        assertEquals(
            MonarchPricingTableLayout.Grid,
            MonarchPricingTableTokens.layout(MonarchPricingTableLayout.Grid, 360f, 2f),
        )
        assertEquals(listOf(null, "Security"), MonarchPricingTableTokens.groups(features).map { it.first })
        assertEquals(MonarchPricingFeatureValue.NotIncluded, MonarchPricingTableTokens.value(features[1], starter))
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(
                colors.textSubtle,
                MonarchPricingTableTokens.valueColor(MonarchPricingFeatureValue.NotIncluded, colors),
                theme.id,
            )
            assertEquals(
                colors.textDefault,
                MonarchPricingTableTokens.valueColor(MonarchPricingFeatureValue.Included, colors),
                theme.id,
            )
            assertEquals(colors.accentBorder, MonarchPricingTableTokens.headerBorder(true, colors), theme.id)
            assertEquals(colors.borderDefault, MonarchPricingTableTokens.headerBorder(false, colors), theme.id)
        }
    }
}
