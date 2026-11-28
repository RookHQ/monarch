package dev.monarchui.sample.sections.layout

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.billing.plancard.MonarchPlanCardFeature
import dev.monarchui.components.billing.pricingtable.MonarchPricingFeature
import dev.monarchui.components.billing.pricingtable.MonarchPricingFeatureValue
import dev.monarchui.components.billing.pricingtable.MonarchPricingPeriod
import dev.monarchui.components.layout.pricingsection.MonarchPricingSection
import dev.monarchui.components.layout.pricingsection.MonarchPricingSectionPlan
import dev.monarchui.sample.CataloguePage

/** The catalogue page of PricingSection. The period picker changes the prices. */
object MonarchPricingSectionSection : CataloguePage {
    override val title = "PricingSection"

    private val plans = listOf(
        MonarchPricingSectionPlan(
            id = "starter",
            name = "Starter",
            description = "For individuals getting organised.",
            prices = mapOf("monthly" to "₹A", "yearly" to "₹B"),
            features = listOf(
                MonarchPlanCardFeature("Up to 3 projects"),
                MonarchPlanCardFeature("Email support"),
                MonarchPlanCardFeature("Single sign-on", included = false),
            ),
        ),
        MonarchPricingSectionPlan(
            id = "team",
            name = "Team",
            description = "For teams that share work every day.",
            prices = mapOf("monthly" to "₹C", "yearly" to "₹D"),
            badge = "Recommended",
            highlighted = true,
            featuresLabel = "Everything in Starter, plus",
            features = listOf(
                MonarchPlanCardFeature("Unlimited projects"),
                MonarchPlanCardFeature("Single sign-on"),
                MonarchPlanCardFeature("Audit log"),
            ),
        ),
        MonarchPricingSectionPlan(
            id = "enterprise",
            name = "Enterprise",
            description = "For organisations with procurement and security reviews.",
            price = "Contact us",
            featuresLabel = "Everything in Team, plus",
            features = listOf(
                MonarchPlanCardFeature("Custom contract"),
                MonarchPlanCardFeature("Dedicated onboarding"),
            ),
        ),
    )

    @Composable
    override fun Content() {
        var period by rememberSaveable { mutableStateOf("monthly") }
        MonarchPricingSection(
            plans = plans,
            title = "Plans that grow with your team",
            eyebrow = "Pricing",
            description = "Placeholder prices for documentation; pass the prices your billing system publishes.",
            periods = listOf(
                MonarchPricingPeriod("monthly", "Monthly", priceSuffix = "/ month"),
                MonarchPricingPeriod("yearly", "Yearly", priceSuffix = "/ year", priceNote = "Billed yearly"),
            ),
            period = period,
            onPeriodChange = { period = it },
            comparison = listOf(
                MonarchPricingFeature(
                    "projects",
                    "Projects",
                    values = mapOf(
                        "starter" to MonarchPricingFeatureValue.Text("Up to 3"),
                        "team" to MonarchPricingFeatureValue.Included,
                        "enterprise" to MonarchPricingFeatureValue.Included,
                    ),
                ),
                MonarchPricingFeature(
                    "sso",
                    "Single sign-on",
                    group = "Security",
                    values = mapOf(
                        "starter" to MonarchPricingFeatureValue.NotIncluded,
                        "team" to MonarchPricingFeatureValue.Included,
                        "enterprise" to MonarchPricingFeatureValue.Included,
                    ),
                ),
            ),
            note = "Prices shown before taxes.",
        ) { plan ->
            MonarchButton(
                text = when (plan.id) {
                    "starter" -> "Choose Starter"
                    "team" -> "Choose Team"
                    else -> "Talk to sales"
                },
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = if (plan.highlighted) MonarchButtonVariant.Primary else MonarchButtonVariant.Secondary,
            )
        }
    }
}
