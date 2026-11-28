package com.rookhq.monarch.components.layout

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.billing.plancard.MonarchPlanCardFeature
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingFeature
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingFeatureValue
import com.rookhq.monarch.components.billing.pricingtable.MonarchPricingPeriod
import com.rookhq.monarch.components.layout.marketingsection.MonarchMarketingAlign
import com.rookhq.monarch.components.layout.marketingsection.MonarchMarketingSectionTone
import com.rookhq.monarch.components.layout.pricingsection.MonarchPricingSection
import com.rookhq.monarch.components.layout.pricingsection.MonarchPricingSectionPlan
import com.rookhq.monarch.components.layout.pricingsection.MonarchPricingSectionTokens
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

private val Periods = listOf(
    MonarchPricingPeriod("monthly", "Monthly", priceSuffix = "/ month"),
    MonarchPricingPeriod("yearly", "Yearly", priceSuffix = "/ year", priceNote = "Billed yearly"),
)
private val Plans = listOf(
    MonarchPricingSectionPlan(
        id = "starter",
        name = "Starter",
        prices = mapOf("monthly" to "₹A", "yearly" to "₹B"),
        features = listOf(
            MonarchPlanCardFeature("Up to 3 projects"),
            MonarchPlanCardFeature("Single sign-on", included = false),
        ),
    ),
    MonarchPricingSectionPlan(
        id = "enterprise",
        name = "Enterprise",
        price = "Contact us",
        badge = "Custom",
        highlighted = true,
    ),
)
private val Comparison = listOf(
    MonarchPricingFeature(
        "sso",
        "Single sign-on",
        values = mapOf(
            "starter" to MonarchPricingFeatureValue.NotIncluded,
            "enterprise" to MonarchPricingFeatureValue.Included,
        ),
    ),
)

internal object MonarchPricingSectionFixtures : ComponentFixtures {
    override val id = "pricing-section"
    override val fixtures = listOf(
        fixture("header, periods and cards", tall = true) {
            MonarchPricingSection(
                Plans,
                title = "Plans that grow with your team",
                eyebrow = "Pricing",
                periods = Periods,
            ) {
                MonarchButton("Choose ${it.name}", {}, Modifier.fillMaxWidth())
            }
        },
        fixture("start aligned, subtle, with comparison", tall = true) {
            MonarchPricingSection(
                Plans.take(1),
                title = "Pricing",
                align = MonarchMarketingAlign.Start,
                tone = MonarchMarketingSectionTone.Subtle,
                comparison = Comparison,
                note = "Prices shown before taxes.",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchPricingSectionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun thePeriodPickerChangesThePrices() {
        var period = ""
        rule.setMonarchContent { MonarchPricingSection(Plans, periods = Periods, onPeriodChange = { period = it }) }
        rule.onNodeWithText("₹A", substring = true).assert(hasText("/ month", substring = true))
        rule.onNodeWithContentDescription("Billing period").assertExists()
        rule.onNodeWithText("Yearly").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals("yearly", period)
        rule.onNodeWithText("₹B", substring = true).assertExists()
    }

    @Test
    fun aPlanWithoutAPriceForThePeriodShowsItsFallback() {
        rule.setMonarchContent { MonarchPricingSection(Plans, periods = Periods) }
        rule.onNodeWithText("Contact us", substring = true).assert(!hasText("/ month", substring = true))
    }

    @Test
    fun theHeaderIsAHeadingAndActionsCallBack() {
        var chosen = ""
        rule.setMonarchContent {
            MonarchPricingSection(
                Plans,
                title = "Plans",
            ) { plan -> MonarchButton("Choose ${plan.name}", { chosen = plan.id }) }
        }
        rule.onNodeWithText("Plans").assert(isHeading())
        rule.onNodeWithText("Choose Starter").assertHasClickAction().performClick()
        assertEquals("starter", chosen)
    }

    @Test
    fun theComparisonHasItsDefaultTitle() {
        rule.setMonarchContent { MonarchPricingSection(Plans, comparison = Comparison) }
        rule.onNodeWithText("Compare plans").assert(isHeading())
    }

    @Test
    fun theComparisonTitleCanBeReplaced() {
        rule.setMonarchContent { MonarchPricingSection(Plans, comparison = Comparison, comparisonTitle = "Comparar") }
        rule.onNodeWithText("Comparar").assertExists()
    }

    @Test
    fun periodsFollowTheLayoutDirection() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchPricingSection(Plans, periods = Periods) }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Monthly").assertIsBefore(rule.onNodeWithText("Yearly"), direction)
        }
    }

    @Test
    fun textWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchPricingSection(
                Plans.take(1),
                title = "Plans that grow with your team",
                note = "Prices shown before taxes.",
            )
        }
        rule.onNodeWithText("Plans that grow with your team").assertTextFullyVisible()
    }

    @Test
    fun theNoteIsMutedInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchPricingSectionTokens.note(colors), theme.id)
        }
    }
}
