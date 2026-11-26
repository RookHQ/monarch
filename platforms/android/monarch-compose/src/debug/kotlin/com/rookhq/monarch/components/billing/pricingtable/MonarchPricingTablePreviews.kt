package com.rookhq.monarch.components.billing.pricingtable

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val previewPlans = listOf(
    MonarchPricingPlan("starter", "Starter", prices = mapOf("month" to "$0", "year" to "$0")),
    MonarchPricingPlan(
        "team",
        "Team",
        description = "For growing teams",
        prices = mapOf("month" to "$12", "year" to "$120"),
        badge = "Recommended",
        highlighted = true,
    ),
    MonarchPricingPlan("enterprise", "Enterprise", price = "Contact us", current = true),
)
private val previewFeatures = listOf(
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
        group = "Security",
        values = mapOf(
            "team" to MonarchPricingFeatureValue.Included,
            "enterprise" to MonarchPricingFeatureValue.Included,
        ),
    ),
)
private val previewPeriods = listOf(
    MonarchPricingPeriod("month", "Monthly", priceSuffix = "/ month"),
    MonarchPricingPeriod("year", "Yearly", priceSuffix = "/ year", priceNote = "Billed yearly"),
)

@MonarchPreviews
@Composable
internal fun MonarchPricingTablePreview() {
    PreviewFrame {
        MonarchPricingTable(
            plans = previewPlans,
            features = previewFeatures,
            periods = previewPeriods,
            layout = MonarchPricingTableLayout.Paged,
        ) { plan -> MonarchButton(text = "Choose ${plan.name}", onClick = {}) }
    }
}

@MonarchPreviews
@Composable
internal fun MonarchPricingTableGridPreview() {
    PreviewFrame {
        MonarchPricingTable(
            plans = previewPlans,
            features = previewFeatures,
            periods = previewPeriods,
            layout = MonarchPricingTableLayout.Grid,
        )
    }
}
