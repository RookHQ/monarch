package dev.monarchui.components.layout.pricingsection

import androidx.compose.runtime.Composable
import dev.monarchui.components.billing.plancard.MonarchPlanCardFeature
import dev.monarchui.components.billing.pricingtable.MonarchPricingPeriod
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchPricingSectionPreview() {
    PreviewFrame {
        MonarchPricingSection(
            plans = listOf(
                MonarchPricingSectionPlan(
                    id = "team",
                    name = "Team",
                    description = "For teams that share work every day.",
                    prices = mapOf("monthly" to "₹C", "yearly" to "₹D"),
                    badge = "Recommended",
                    highlighted = true,
                    features = listOf(MonarchPlanCardFeature("Unlimited projects")),
                ),
            ),
            title = "Plans that grow with your team",
            eyebrow = "Pricing",
            periods = listOf(
                MonarchPricingPeriod("monthly", "Monthly", priceSuffix = "/ month"),
                MonarchPricingPeriod("yearly", "Yearly", priceSuffix = "/ year"),
            ),
            note = "Prices shown before taxes.",
        )
    }
}
